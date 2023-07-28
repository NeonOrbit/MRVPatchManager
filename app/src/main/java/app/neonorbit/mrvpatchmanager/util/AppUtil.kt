package app.neonorbit.mrvpatchmanager.util

import android.content.Context
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object AppUtil {
    fun prompt(
        context: Context,
        @StringRes title: Int? = null,
        @StringRes message: Int? = null,
        @StringRes positive: Int? = null,
        block: ((Boolean) -> Unit)
    ) {
        prompt(context,
            title?.let { context.getString(it) },
            message?.let { context.getString(it) },
            positive?.let { context.getString(it) },
            block
        )
    }

    fun prompt(
        context: Context,
        title: String? = null,
        message: String? = null,
        positive: String? = null,
        block: ((Boolean) -> Unit)
    ) {
        var result = false
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(
                positive ?: context.getString(android.R.string.ok)
            ) { _,_-> result = true }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener { block.invoke(result) }
            .show()
    }

    fun prompt(context: Context, @StringRes message: Int, vararg formatArgs: Any) {
        prompt(context, context.getString(message, *formatArgs))
    }

    fun prompt(context: Context, message: String) {
        MaterialAlertDialogBuilder(context).setMessage(message)
            .setNegativeButton(android.R.string.ok, null)
            .show()
    }

    fun show(context: Context, @StringRes message: Int) {
        show(context, context.getString(message))
    }

    private fun show(context: Context, message: String) {
        MaterialAlertDialogBuilder(context).setMessage(message).show()
    }
}
