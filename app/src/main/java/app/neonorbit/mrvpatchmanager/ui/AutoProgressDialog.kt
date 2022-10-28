package app.neonorbit.mrvpatchmanager.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.UiThread
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.neonorbit.mrvpatchmanager.databinding.SimpleProgressViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AutoProgressDialog : DialogFragment() {
    private var viewModel: AutoProgressViewModel? = null

    @UiThread
    fun post(parent: Fragment, title: String? = null, progress: Int?) {
        progress?.let {
            if (!isAdded(parent)) {
                if (title != null) setTitle(title)
                showNow(parent.childFragmentManager, TAG)
            }
            viewModel?.liveProgress?.value = it
        } ?: finish(parent)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = SimpleProgressViewBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[AutoProgressViewModel::class.java]
        viewModel?.liveProgress?.observe(this) {
            binding.progressBar.progress = it
            binding.progressBar.isIndeterminate = it < 0
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(arguments?.getString(TITLE))
            .setView(binding.root)
            .setNegativeButton(getString(android.R.string.cancel)) { _,_->
                listener?.onCancel()
            }.create()
    }

    override fun dismiss() {
        super.dismissNow()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener?.onCancel()
    }

    private fun setTitle(title: String) {
        arguments?.putString(TITLE, title)
    }

    private fun finish(parent: Fragment) {
        getFragment(parent)?.dismissNow()
    }

    interface OnCancelListener {
        fun onCancel()
    }

    private val listener: OnCancelListener? get() = parentFragment?.let {
        if (it is OnCancelListener) it else null
    }

    private fun isAdded(parent: Fragment): Boolean {
        return getFragment(parent)?.isAdded == true
    }

    private fun getFragment(parent: Fragment): AutoProgressDialog? {
        return parent.childFragmentManager.findFragmentByTag(TAG)?.let {
            it as AutoProgressDialog
        }
    }

    companion object {
        private const val TAG = "QuickProgress"
        private const val TITLE = "TitleArgKey"

        fun newInstance(title: String = "Progress"): AutoProgressDialog {
            return AutoProgressDialog().apply {
                isCancelable = false
                arguments = Bundle()
                setTitle(title)
            }
        }
    }

    class AutoProgressViewModel : ViewModel() {
        val liveProgress: MutableLiveData<Int> = MutableLiveData()
    }
}
