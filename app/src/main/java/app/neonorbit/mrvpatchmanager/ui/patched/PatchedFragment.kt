package app.neonorbit.mrvpatchmanager.ui.patched

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.LinearLayoutManager
import app.neonorbit.mrvpatchmanager.util.AppUtil
import app.neonorbit.mrvpatchmanager.AppInstaller
import app.neonorbit.mrvpatchmanager.R
import app.neonorbit.mrvpatchmanager.databinding.FragmentPatchedBinding
import app.neonorbit.mrvpatchmanager.glide.RecyclerPreloadProvider
import app.neonorbit.mrvpatchmanager.repository.data.ApkFileData
import app.neonorbit.mrvpatchmanager.withLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import java.io.File

class PatchedFragment : Fragment(), ActionMode.Callback, ApkListAdapter.Callback {
    private var binding: FragmentPatchedBinding? = null
    private var viewModel: PatchedViewModel? = null
    private var tracker: SelectionTracker<Long>? = null
    private var actionMode: ActionMode? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPatchedBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[PatchedViewModel::class.java]

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

        val sizeProvider = ViewPreloadSizeProvider<ApkFileData>()
        val modelProvider = RecyclerPreloadProvider(this, recyclerAdapter.items)
        val viewPreLoader = RecyclerViewPreloader(
            Glide.with(this), modelProvider, sizeProvider, 20
        )
        recyclerView.addOnScrollListener(viewPreLoader)

        initializeFragment(binding!!, viewModel!!)
        viewModel?.reloadPatchedApks()
        return binding!!.root
    }

    private fun initializeFragment(binding: FragmentPatchedBinding, model: PatchedViewModel) {
        model.patchedApkList.observe(viewLifecycleOwner) {
            recyclerAdapter?.reloadItems(it)
            binding.progressBar.isVisible = false
            binding.emptyView.isVisible = it.isEmpty()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        model.progressState.observe(viewLifecycleOwner) {
            binding.progressBar.isVisible = it
        }

        viewLifecycleOwner.withLifecycle(Lifecycle.State.STARTED) {
            model.confirmationEvent.observe { event ->
                AppUtil.prompt(requireContext(), event.title, event.message) {
                    event.response(it)
                }
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            model.reloadPatchedApks()
        }

        binding.swipeRefreshLayout.setColorSchemeColors(
            requireContext().getColor(R.color.secondary_container_foreground)
        )

        binding.swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
            requireContext().getColor(R.color.secondary_container)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        actionMode?.finish()
        actionMode = null
        viewModel = null
        binding = null
    }

    override fun onItemClicked(item: ApkFileData) {
        AppInstaller.install(requireContext(), File(item.path))
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
            R.id.selectAll -> {
                recyclerAdapter?.let {
                    tracker?.setItemsSelected(it.getItemIds(), true)
                    it.refresh()
                }
                return true
            }
            R.id.delete -> {
                recyclerAdapter?.let { adapter ->
                    tracker?.selection?.map { adapter.items[it.toInt()] }?.let {
                        viewModel?.deletePatchedApks(it)
                    }
                }
                tracker?.clearSelection()
                return true
            }
            else -> false
        }
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
