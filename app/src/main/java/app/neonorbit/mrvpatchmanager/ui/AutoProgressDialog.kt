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
    companion object {
        private const val TITLE = "TitleArgKey"
        private const val INTERACTIVE = "Interactive"

        @UiThread
        fun post(parent: Fragment, tag: String, title: String?, progress: Int?, interactive: Boolean = true) {
            val instance = parent.childFragmentManager.findFragmentByTag(tag)?.let { it as AutoProgressDialog }
            if (progress == null) {
                instance?.dismiss()
                return
            }
            ViewModelProvider(parent)[AutoProgressViewModel::class.java].liveProgress.value = progress
            if (instance == null || (instance.dialog?.isShowing != true && !parent.childFragmentManager.isStateSaved)) {
                instance?.dismiss()
                AutoProgressDialog().apply {
                    isCancelable = false
                    arguments = Bundle()
                    setTitle(title ?: "Progress")
                    setInteractive(interactive)
                    showNow(parent.childFragmentManager, tag)
                }
            } else {
                instance.dialog?.setTitle(title)
            }
        }
    }

    class AutoProgressViewModel : ViewModel() {
        val liveProgress: MutableLiveData<Int> = MutableLiveData()
    }

    private val viewModel by lazy {
        ViewModelProvider(requireParentFragment())[AutoProgressViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = SimpleProgressViewBinding.inflate(layoutInflater)
        viewModel.liveProgress.observe(this) {
            binding.progressBar.progress = it
            binding.progressBar.isIndeterminate = it < 0
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(arguments?.getString(TITLE))
            .setView(binding.root)
            .also {
                if (isInteractive()) it.setNegativeButton(getString(android.R.string.cancel)) { _,_->
                    listener?.onProgressCancelled()
                }
            }.create()
    }

    override fun dismiss() {
        super.dismissAllowingStateLoss()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener?.onProgressCancelled()
    }

    interface OnCancelListener {
        fun onProgressCancelled()
    }

    private val listener: OnCancelListener? get() = parentFragment as? OnCancelListener

    private fun setTitle(title: String) {
        arguments?.putString(TITLE, title)
    }

    private fun setInteractive(interactive: Boolean) {
        arguments?.putBoolean(INTERACTIVE, interactive)
    }

    private fun isInteractive(): Boolean {
        return arguments?.getBoolean(INTERACTIVE) != false
    }
}
