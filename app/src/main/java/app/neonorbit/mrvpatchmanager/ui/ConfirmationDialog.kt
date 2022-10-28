package app.neonorbit.mrvpatchmanager.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ConfirmationDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(arguments?.getString(TITLE))
            .setMessage(arguments?.getString(MESSAGE))
            .setPositiveButton(
                arguments?.getString(POSITIVE) ?:
                requireContext().getString(android.R.string.ok)
            ) { _,_->
                listener?.onResponse(true)
            }
            .setNegativeButton(
                getString(android.R.string.cancel)
            ) { _,_->
                listener?.onResponse(false)
            }
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener?.onResponse(false)
    }

    interface ResponseListener {
        fun onResponse(response: Boolean)
    }

    private val listener: ResponseListener? get() = parentFragment?.let {
        if (it is ResponseListener) it else null
    }

    companion object {
        private const val TAG = "Confirmation"
        private const val TITLE = "TitleArgKey"
        private const val MESSAGE = "MessageArgKey"
        private const val POSITIVE = "PositiveArgKey"

        fun show(
            parent: Fragment,
            title: String? = null,
            message: String? = null,
            positive: String? = null
        ) {
            ConfirmationDialog().apply {
                arguments = Bundle().apply {
                    putString(TITLE, title)
                    putString(MESSAGE, message)
                    putString(POSITIVE, positive)
                }
            }.show(parent.childFragmentManager, TAG)
        }
    }
}
