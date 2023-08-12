package app.neonorbit.mrvpatchmanager.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import app.neonorbit.mrvpatchmanager.AppConfig
import app.neonorbit.mrvpatchmanager.AppInstaller
import app.neonorbit.mrvpatchmanager.R
import app.neonorbit.mrvpatchmanager.data.AppFileData
import app.neonorbit.mrvpatchmanager.data.AppItemData
import app.neonorbit.mrvpatchmanager.databinding.FragmentHomeBinding
import app.neonorbit.mrvpatchmanager.databinding.InstalledAppsDialogBinding
import app.neonorbit.mrvpatchmanager.databinding.VersionInputDialogBinding
import app.neonorbit.mrvpatchmanager.event.UpdateEvent
import app.neonorbit.mrvpatchmanager.observeOnUI
import app.neonorbit.mrvpatchmanager.ui.AutoProgressDialog
import app.neonorbit.mrvpatchmanager.ui.ConfirmationDialog
import app.neonorbit.mrvpatchmanager.util.AppUtil
import app.neonorbit.mrvpatchmanager.withLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.StringJoiner

class HomeFragment : Fragment(),
    ActivityResultCallback<ActivityResult>,
    AutoProgressDialog.OnCancelListener,
    ConfirmationDialog.ResponseListener
{
    private var binding: FragmentHomeBinding? = null
    private var viewModel: HomeViewModel? = null
    private var intentLauncher: ActivityResultLauncher<Intent>? = null
    private val uriLauncher by lazy { CustomTabsIntent.Builder().build() }
    private val autoProgressDialog by lazy { AutoProgressDialog.newInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        intentLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(), this
        )
        initializeFragment(binding!!, viewModel!!)
        return binding!!.root
    }

    private fun initializeFragment(binding: FragmentHomeBinding, model: HomeViewModel) {
        val context = requireContext()

        model.moduleStatus.observeOnUI(viewLifecycleOwner) {
            updateModuleCard(it)
        }
        model.reloadModuleStatus()

        binding.dropDownMenu.setAdapter(AppsAdapter(context, model.fbAppList))
        binding.dropDownMenu.setText(model.fbAppList[0].name)

        binding.dropDownMenu.setOnItemClickListener { _, selected, _, _ ->
            binding.patchButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, null, (selected as TextView).compoundDrawables[0], null
            )
        }

        model.quickDownloadProgress.observeOnUI(viewLifecycleOwner) { progress ->
            autoProgressDialog.post(this, "Downloading", progress)
        }

        model.progressStatus.observeOnUI(viewLifecycleOwner) {
            binding.progressView.isVisible = it?.let {
                binding.progressStatus.text = it
            } != null
        }

        model.progressTracker.observeOnUI(viewLifecycleOwner) {
            binding.progressDetails.text = it.details
            binding.progressPercent.text = it.percent
            val animate = !binding.progressBar.isIndeterminate
            binding.progressBar.isIndeterminate = it.current < 0
            if (!binding.progressBar.isIndeterminate) {
                binding.progressBar.setProgressCompat(it.current, animate)
            }
        }

        model.patchingStatus.observeOnUI(viewLifecycleOwner) {
            binding.dropDown.isEnabled = !it
            binding.patchButton.isClickable = true
            binding.patchButton.text = if (!it) "Patch" else "Cancel"
        }

        binding.patchButton.setOnClickListener {
            selectedApp?.let {
                binding.dropDown.isEnabled = false
                binding.patchButton.isClickable = false
                model.patch(it.type)
            }
        }

        binding.versionButton.setOnClickListener {
            showVersionInputDialog()
        }

        binding.patcherManual.setOnClickListener {
            PopupMenu(requireContext(), it).apply {
                menuInflater.inflate(R.menu.manual_patch_menu, menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.pick_apk -> model.manualRequest(true)
                        R.id.pick_app -> model.manualRequest(false)
                    }
                    true
                }
            }.show()
        }

        viewLifecycleOwner.withLifecycle(Lifecycle.State.STARTED) {
            model.appPickerEvent.observe { apps ->
                showInstalledAppPickerDialog(apps)
            }
        }

        viewLifecycleOwner.withLifecycle(Lifecycle.State.STARTED) {
            model.messageEvent.observe { message ->
                AppUtil.prompt(context, message)
            }
        }

        viewLifecycleOwner.withLifecycle(Lifecycle.State.STARTED) {
            model.confirmationEvent.observe { event ->
                ConfirmationDialog.show(
                    this@HomeFragment, event.title, event.message
                )
            }
        }

        viewLifecycleOwner.withLifecycle(Lifecycle.State.STARTED) {
            model.intentEvent.observe { intent ->
                intentLauncher?.launch(intent)
            }
        }

        viewLifecycleOwner.withLifecycle(Lifecycle.State.STARTED) {
            model.uriEvent.observe {
                uriLauncher.launchUrl(requireContext(), it)
            }
        }

        viewLifecycleOwner.withLifecycle(Lifecycle.State.STARTED) {
            model.installEvent.observe { file ->
                AppInstaller.install(requireContext(), file)
            }
        }

        viewLifecycleOwner.withLifecycle(Lifecycle.State.STARTED) {
            model.uninstallEvent.observe { packages ->
                packages.forEach {
                    AppInstaller.uninstall(requireContext(), it)
                }
            }
        }

        model.getPatcherOptions().let { opt ->
            StringJoiner("\n").apply {
                if (opt.maskPackage) add(getEnabledString(R.string.pref_mask_package_title))
                if (opt.fixConflict) add(getEnabledString(R.string.pref_fix_conflict_title))
                if (opt.fallbackMode) add(getEnabledString(R.string.pref_fallback_mode_title))
            }.toString().takeIf { it.isNotEmpty() }?.let {
                binding.noticeCard.isVisible = true
                binding.noticeCardText.text = it
            } ?: { binding.noticeCard.isVisible = false }
        }

        binding.managerButton.setOnClickListener { model.installManager() }
        binding.moduleInfoButton.setOnClickListener { model.showModuleInfo() }
        binding.moduleUpdateButton.setOnClickListener { model.installModule() }
        binding.moduleInstallButton.setOnClickListener { model.installModule(true) }
        binding.managerChangelogButton.setOnClickListener { model.visitManager() }
        binding.moduleChangelogButton.setOnClickListener { model.visitModule() }
    }

    private fun showVersionInputDialog() {
        VersionInputDialogBinding.inflate(LayoutInflater.from(requireContext()), null, false).let { bind ->
            MaterialAlertDialogBuilder(requireContext()).setView(bind.root).create().also { dialog ->
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
    private fun getEnabledString(@StringRes resId: Int) = "[Enabled] -> [${getString(resId)}]"

    override fun onActivityResult(result: ActivityResult?) {
        if (result?.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let {
                viewModel?.patch(uri = it)
            }
        }
    }

    override fun onCancel() {
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
        intentLauncher?.unregister()
        intentLauncher = null
        viewModel = null
        binding = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onInstallationEvent(data: AppInstaller.Event) {
        if (data.pkg == AppConfig.MODULE_PACKAGE) {
            EventBus.getDefault().removeStickyEvent(data)
            viewModel?.reloadModuleStatus(true)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onUpdateEvent(event: UpdateEvent) {
        when (event) {
            is UpdateEvent.Manager -> {
                updateManagerCard(event.current, event.latest)
            }
            is UpdateEvent.Module -> {
                viewModel?.updateModuleStatus(event.current, event.latest)
            }
        }
    }

    private fun updateManagerCard(current: String, latest: String) {
        binding!!.managerLatestSubtitle.text = requireContext().getString(
            R.string.latest_version, latest
        )
        binding!!.managerInstalledSubtitle.text = requireContext().getString(
            R.string.installed_version, current
        )
        binding!!.managerCard.isVisible = true
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
                binding!!.moduleWarning.isVisible = !installed
            }
        }
        binding!!.moduleInstalledSubtitle.text = requireContext().getString(
            R.string.installed_version, status.current ?: "none"
        )
    }
}
