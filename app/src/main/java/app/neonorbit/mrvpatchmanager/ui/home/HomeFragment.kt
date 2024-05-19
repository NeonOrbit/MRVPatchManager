package app.neonorbit.mrvpatchmanager.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.widget.PopupMenu
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import app.neonorbit.mrvpatchmanager.AppConfigs
import app.neonorbit.mrvpatchmanager.AppInstaller
import app.neonorbit.mrvpatchmanager.R
import app.neonorbit.mrvpatchmanager.data.AppFileData
import app.neonorbit.mrvpatchmanager.data.AppItemData
import app.neonorbit.mrvpatchmanager.data.UpdateEventData
import app.neonorbit.mrvpatchmanager.databinding.FragmentHomeBinding
import app.neonorbit.mrvpatchmanager.databinding.InstalledAppsDialogBinding
import app.neonorbit.mrvpatchmanager.databinding.VersionInputDialogBinding
import app.neonorbit.mrvpatchmanager.observeOnUI
import app.neonorbit.mrvpatchmanager.ui.AutoProgressDialog
import app.neonorbit.mrvpatchmanager.ui.ConfirmationDialog
import app.neonorbit.mrvpatchmanager.util.AppUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class HomeFragment : Fragment(),
    AutoProgressDialog.OnCancelListener,
    ConfirmationDialog.ResponseListener
{
    private var binding: FragmentHomeBinding? = null
    private var viewModel: HomeViewModel? = null
    private lateinit var apkPicker: ActivityResultLauncher<Intent>
    private val uriLauncher by lazy { CustomTabsIntent.Builder().build() }
    private val autoProgressDialog by lazy { AutoProgressDialog.newInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        apkPicker = registerForActivityResult(StartActivityForResult()) { result ->
            result?.data?.data?.let {
                viewModel?.patch(uri = it)
            }
        }
        initializeFragment()
        return binding!!.root
    }

    private fun initializeFragment() {
        viewModel!!.moduleStatus.observeOnUI(viewLifecycleOwner) {
            updateModuleCard(it)
        }
        viewModel!!.reloadModuleStatus()

        binding!!.dropDownMenu.setAdapter(AppsAdapter(requireContext(), viewModel!!.fbAppList))
        binding!!.dropDownMenu.setText(viewModel!!.fbAppList[0].name)

        binding!!.dropDownMenu.setOnItemClickListener { _, selected, _, _ ->
            binding!!.patchButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, null, (selected as TextView).compoundDrawables[0], null
            )
        }

        viewModel!!.quickDownloadProgress.observeOnUI(viewLifecycleOwner) { progress ->
            autoProgressDialog.post(this, "Downloading", progress)
        }

        viewModel!!.progressStatus.observeOnUI(viewLifecycleOwner) {
            binding!!.progressView.isVisible = it?.let {
                binding!!.progressStatus.text = it
            } != null
        }

        viewModel!!.progressTracker.observeOnUI(viewLifecycleOwner) {
            binding!!.progressDetails.text = it.details
            binding!!.progressPercent.text = it.percent
            val animate = !binding!!.progressBar.isIndeterminate
            binding!!.progressBar.isIndeterminate = it.current < 0
            if (!binding!!.progressBar.isIndeterminate) {
                binding!!.progressBar.setProgressCompat(it.current, animate)
            }
        }

        viewModel!!.patchingStatus.observeOnUI(viewLifecycleOwner) {
            binding!!.dropDown.isEnabled = !it
            binding!!.patchButton.isClickable = true
            binding!!.patchButton.text = if (!it) "Patch" else "Cancel"
        }

        binding!!.patchButton.setOnClickListener {
            selectedApp?.let {
                binding!!.dropDown.isEnabled = false
                binding!!.patchButton.isClickable = false
                viewModel?.patch(it.type)
            }
        }

        binding!!.versionButton.setOnClickListener {
            showVersionInputDialog()
        }

        binding!!.patcherManual.setOnClickListener {
            PopupMenu(requireContext(), it).apply {
                menuInflater.inflate(R.menu.manual_patch_menu, menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.pick_apk -> viewModel?.manualRequest(true)
                        R.id.pick_app -> viewModel?.manualRequest(false)
                    }
                    true
                }
            }.show()
        }

        viewModel!!.intentEvent.observeOnUI(viewLifecycleOwner) { intent ->
            startActivity(intent)
        }

        viewModel!!.apkPickerEvent.observeOnUI(viewLifecycleOwner) { intent ->
            apkPicker.launch(intent)
        }

        viewModel!!.appPickerEvent.observeOnUI(viewLifecycleOwner) { apps ->
            showInstalledAppPickerDialog(apps)
        }

        viewModel!!.messageEvent.observeOnUI(viewLifecycleOwner) { message ->
            AppUtil.prompt(requireContext(), message)
        }

        viewModel!!.confirmationEvent.observeOnUI(viewLifecycleOwner) { event ->
            ConfirmationDialog.show(
                this@HomeFragment, event.title, event.message, event.action
            )
        }

        viewModel!!.uriEvent.observeOnUI(viewLifecycleOwner) {
            uriLauncher.launchUrl(requireContext(), it)
        }

        viewModel!!.installEvent.observeOnUI(viewLifecycleOwner) { file ->
            AppInstaller.install(requireContext(), file)
        }

        viewModel!!.uninstallEvent.observeOnUI(viewLifecycleOwner) { packages ->
            packages.forEach {
                AppInstaller.uninstall(requireContext(), it)
            }
        }

        viewModel!!.getNotice()?.let {
            binding!!.noticeCard.isVisible = true
            binding!!.noticeCardText.text = it
        } ?: { binding!!.noticeCard.isVisible = false }

        binding!!.managerButton.setOnClickListener { viewModel!!.installManager() }
        binding!!.moduleInfoButton.setOnClickListener { viewModel!!.showModuleInfo() }
        binding!!.moduleUpdateButton.setOnClickListener { viewModel!!.installModule() }
        binding!!.moduleInstallButton.setOnClickListener { viewModel!!.installModule(true) }
        binding!!.managerChangelogButton.setOnClickListener { viewModel!!.visitManager() }
        binding!!.moduleChangelogButton.setOnClickListener { viewModel!!.visitModule() }
    }

    private fun showVersionInputDialog() {
        VersionInputDialogBinding.inflate(LayoutInflater.from(requireContext()), null, false).let { bind ->
            MaterialAlertDialogBuilder(requireContext()).setView(bind.root).create().also { dialog ->
                bind.apkVersion.editText!!.setText(viewModel?.lastSelectedVersion)
                bind.patchButton.setOnClickListener {
                    dialog.dismiss()
                    selectedApp?.let {
                        viewModel?.patchVersion(it.type, bind.apkVersion.editText!!.text!!.toString())
                    }
                }
            }.show()
        }
    }

    private fun showInstalledAppPickerDialog(apps: List<AppFileData>) {
        InstalledAppsDialogBinding.inflate(LayoutInflater.from(requireContext()), null, false).let { bind ->
            MaterialAlertDialogBuilder(requireContext()).setView(bind.root).create().also { dialog ->
                bind.list.layoutManager = LinearLayoutManager(requireContext())
                bind.list.adapter = InstalledAppAdapter(apps).apply {
                    setItemClickListener { item ->
                        dialog.dismiss()
                        viewModel?.patch(uri = item.file.toUri())
                    }
                }
            }.show()
        }
    }

    private val selectedApp: AppItemData? get() = viewModel?.fbAppList?.find {
        binding?.dropDownMenu?.text.toString() == it.name
    }

    override fun onProgressCancelled() {
        viewModel?.quickDownloadJob?.value?.cancel()
    }

    override fun onResponse(response: Boolean) {
        viewModel?.confirmationEvent?.sendResponse(response)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel = null
        binding = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onInstallationEvent(data: AppInstaller.Event) {
        if (data.pkg == AppConfigs.MODULE_PACKAGE) {
            EventBus.getDefault().removeStickyEvent(data)
            viewModel?.reloadModuleStatus(true)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onUpdateEvent(event: UpdateEventData) {
        when (event) {
            is UpdateEventData.Manager -> {
                updateManagerCard(event.current, event.latest)
            }
            is UpdateEventData.Module -> {
                viewModel?.updateModuleStatus(event.current, event.latest)
            }
        }
    }

    private fun updateManagerCard(current: String, latest: String) {
        binding?.managerLatestSubtitle?.text = requireContext().getString(
            R.string.latest_version, latest
        )
        binding?.managerInstalledSubtitle?.text = requireContext().getString(
            R.string.installed_version, current
        )
        binding?.managerCard?.isVisible = true
    }

    private fun updateModuleCard(status: HomeViewModel.VersionStatus) {
        if (status.latest != null) {
            binding!!.moduleWarning.isVisible = false
            binding!!.moduleInstallButton.isVisible = false
            binding!!.moduleUpdateButton.isVisible = true
            binding!!.moduleLatestSubtitle.isVisible = true
            binding!!.moduleChangelogButton.isVisible = true
            binding!!.moduleLatestSubtitle.text = requireContext().getString(
                R.string.latest_version, status.latest
            )
        } else {
            binding!!.moduleChangelogButton.isVisible = false
            binding!!.moduleLatestSubtitle.isVisible = false
            binding!!.moduleUpdateButton.isVisible = false
            (status.current != null).let { installed ->
                binding!!.moduleInfoButton.isVisible = installed
                binding!!.moduleInstallButton.isVisible = !installed
                binding!!.moduleWarning.isVisible = !installed && viewModel?.hideModuleWarn != true
            }
        }
        binding!!.moduleInstalledSubtitle.text = requireContext().getString(
            R.string.installed_version, status.current ?: "none"
        )
    }
}
