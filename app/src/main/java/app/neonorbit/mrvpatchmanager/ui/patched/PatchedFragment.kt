package app.neonorbit.mrvpatchmanager.ui.patched

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.LinearLayoutManager
import app.neonorbit.mrvpatchmanager.R
import app.neonorbit.mrvpatchmanager.UniversalInstaller
import app.neonorbit.mrvpatchmanager.databinding.FragmentPatchedBinding
import app.neonorbit.mrvpatchmanager.glide.RecyclerPreloadProvider
import app.neonorbit.mrvpatchmanager.repository.data.ApkFileData
import app.neonorbit.mrvpatchmanager.ui.AutoProgressDialog
import app.neonorbit.mrvpatchmanager.ui.ConfirmationDialog
import app.neonorbit.mrvpatchmanager.util.AppUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PatchedFragment : Fragment(),
    ActionMode.Callback, ApkListAdapter.Callback,
    ConfirmationDialog.ResponseListener
{
    private var binding: FragmentPatchedBinding? = null
    private var viewModel: PatchedViewModel? = null
    private var tracker: SelectionTracker<Long>? = null
    private var actionMode: ActionMode? = null
    private lateinit var saveDirPicker: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPatchedBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[PatchedViewModel::class.java]
        saveDirPicker = registerForActivityResult(StartActivityForResult()) { result ->
            result?.data?.let { viewModel?.onSaveDirectoryPicked(it) }
        }
        binding!!.progressBar.isVisible = true

        val recyclerAdapter = ApkListAdapter()
        val recyclerView = binding!!.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recyclerAdapter
            setHasFixedSize(true)
        }
        tracker = recyclerAdapter.initTracker(recyclerView).also {
            registerSelectionObserver(it)
        }
        recyclerAdapter.setItemClickListener(this)
        recyclerAdapter.setApkInfoPreloader(
            ApkInfoPreloader(this, 15, recyclerAdapter.items)
        )

        val sizeProvider = ViewPreloadSizeProvider<ApkFileData>()
        val modelProvider = RecyclerPreloadProvider(this, recyclerAdapter.items)
        val viewPreLoader = RecyclerViewPreloader(
            Glide.with(this), modelProvider, sizeProvider, 20
        )
        recyclerView.addOnScrollListener(viewPreLoader)

        initializeFragment()
        viewModel?.reloadPatchedApks()
        return binding!!.root
    }

    private fun initializeFragment() {
        viewModel!!.patchedApkList.observe(viewLifecycleOwner) {
            recyclerAdapter?.reloadItems(it)
            binding!!.progressBar.isVisible = false
            binding!!.emptyView.isVisible = it.isEmpty()
            binding!!.swipeRefreshLayout.isRefreshing = false
        }

        viewModel!!.progressState.observe(viewLifecycleOwner) {
            binding!!.progressBar.isVisible = it
        }

        viewModel!!.dialogEvent.observeOnUI(viewLifecycleOwner) { message ->
            AppUtil.show(requireContext(), message)
        }

        viewModel!!.intentEvent.observeOnUI(viewLifecycleOwner) { intent ->
            startActivity(intent)
        }

        viewModel!!.saveFilesEvent.observeOnUI(viewLifecycleOwner) { intent ->
            saveDirPicker.launch(intent)
        }

        viewModel!!.confirmationEvent.observeOnUI(viewLifecycleOwner) { event ->
            ConfirmationDialog.show(
                this@PatchedFragment, event.title, event.message, event.action
            )
        }

        binding!!.swipeRefreshLayout.setOnRefreshListener {
            viewModel!!.reloadPatchedApks()
        }

        binding!!.swipeRefreshLayout.setColorSchemeColors(
            requireContext().getColor(R.color.secondary_container_foreground)
        )

        binding!!.swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
            requireContext().getColor(R.color.secondary_container)
        )
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
        actionMode?.finish()
        actionMode = null
        viewModel = null
        binding = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onInstallationEvent(event: UniversalInstaller.Event) {
        AutoProgressDialog.post(this, "PIE", event.msg, event.msg?.let { -1 }, false)
        if (event.intent != null && UniversalInstaller.isPending()) startActivity(event.intent)
    }

    override fun onItemClicked(item: ApkFileData) {
        viewModel?.installApk(item)
    }

    override fun onResponse(response: Boolean) {
        viewModel?.confirmationEvent?.sendResponse(response)
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.app_bar_context_menu, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.selectAll -> recyclerAdapter?.let { adapter ->
                tracker?.setItemsSelected(adapter.getItemIds(), true)
                adapter.refresh()
            }
            R.id.save -> viewModel?.saveSelectedApks(getSelectedFiles())
            R.id.share -> viewModel?.shareSelectedApks(getSelectedFiles())
            R.id.delete -> viewModel?.deleteSelectedApks(getSelectedFiles())
            R.id.details -> viewModel?.showDetails(getSelectedFiles())
            else -> null
        }?.let {
            if (item.itemId != R.id.selectAll) tracker?.clearSelection()
        } != null
    }

    private fun getSelectedFiles(): List<ApkFileData> {
        return recyclerAdapter?.let { adapter ->
            tracker?.selection?.map { adapter.items[it.toInt()] }
        } ?: listOf()
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        tracker?.clearSelection()
        recyclerAdapter?.refresh()
    }

    private fun registerSelectionObserver(tracker: SelectionTracker<Long>) {
        tracker.addObserver(object: SelectionTracker.SelectionObserver<Long>() {
            override fun onSelectionChanged() {
                actionMode = if (tracker.hasSelection()) {
                    actionMode ?: startActionMode()
                } else {
                    actionMode?.finish()
                    null
                }
                actionMode?.let {
                    it.title = tracker.selection.size().toString()
                }
                binding?.swipeRefreshLayout?.isEnabled = !tracker.hasSelection()
            }
        })
    }

    private val recyclerAdapter: ApkListAdapter? get() {
        return binding?.recyclerView?.adapter as ApkListAdapter?
    }

    private fun startActionMode(): ActionMode? {
        return (requireActivity() as AppCompatActivity).startSupportActionMode(this)
    }
}
