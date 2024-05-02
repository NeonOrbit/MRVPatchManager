package app.neonorbit.mrvpatchmanager.ui.settings

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import app.neonorbit.mrvpatchmanager.R
import app.neonorbit.mrvpatchmanager.databinding.KeystoreDialogBinding
import app.neonorbit.mrvpatchmanager.keystore.KeystoreInputData
import app.neonorbit.mrvpatchmanager.util.AppUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

class KeystoreDialogFragment : DialogFragment() {
    private var binding: KeystoreDialogBinding? = null
    private var launcher: ActivityResultLauncher<Intent>? = null

    interface ResponseListener {
        fun onKeystoreInput(response: KeystoreInputData?)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = KeystoreDialogBinding.inflate(layoutInflater)
        launcher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            result.data?.let { intent ->
                setKeystore(intent.data)
                binding!!.keyfile.value = intent.data?.path?.substringAfterLast('/')
            }
        }
        binding!!.keyfile.editText!!.setOnClickListener {
            launcher?.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
            })
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding!!.root)
            .setPositiveButton(getString(R.string.text_save), null)
            .setNegativeButton(getString(android.R.string.cancel), null)
            .setNeutralButton(getString(R.string.text_reset)) { _, _ ->
                parentFragment?.let { fragment ->
                    AppUtil.prompt(requireContext(), message = "Reset keystore to default?") {
                        if (it && fragment is ResponseListener) fragment.onKeystoreInput(null)
                    }
                }
            }.create().also {
                it.setOnShowListener { _ ->
                    it.getButton(AlertDialog.BUTTON_POSITIVE)!!.setOnClickListener {
                        if (getKeystore() == null) binding!!.keyfile.value = null
                        save()
                    }
                }
            }
    }

    private fun save() {
        binding!!.warning.isVisible = false
        if (!binding!!.keyfile.checkValue()) return
        if (!binding!!.password.checkValue()) return
        parentFragment?.let {
            if (it is ResponseListener) {
                binding!!.progressBar.isVisible = true
                it.onKeystoreInput(
                    KeystoreInputData(
                        getKeystore()!!,
                        binding!!.password.value!!,
                        binding!!.keyAlias.value,
                        binding!!.aliasPassword.value,
                    )
                )
            }
        }
    }

    fun failed(error: String) {
        binding?.warning?.text = error
        binding?.warning?.isVisible = true
        binding?.progressBar?.isVisible = false
    }

    private fun finish() {
        dismissNow()
    }

    private fun TextInputLayout.checkValue(): Boolean {
        error = value?.isNotEmpty().let {
            if (it == true) null else getString(R.string.text_required)
        }
        return error == null
    }

    private var TextInputLayout.value: String?
        get() = editText?.text?.toString()
        set(value) { editText?.setText(value) }

    private fun setKeystore(uri: Uri?) {
        if (arguments == null) arguments = Bundle()
        arguments?.putParcelable(KEYSTORE, uri)
    }

    @Suppress("deprecation")
    private fun getKeystore(): Uri? {
        return try {
            arguments?.getParcelable(KEYSTORE) as Uri?
        } catch (_: Exception) { null }
    }

    companion object {
        private const val TAG = "KeystoreDialog"
        private const val KEYSTORE = "KeystoreUri"

        fun show(parent: Fragment) {
            KeystoreDialogFragment().show(parent.childFragmentManager, TAG)
        }

        fun finish(parent: Fragment) = getFragment(parent)?.finish()

        fun failed(parent: Fragment, error: String) = getFragment(parent)?.failed(error)

        private fun getFragment(parent: Fragment): KeystoreDialogFragment? {
            return parent.childFragmentManager.findFragmentByTag(TAG) as KeystoreDialogFragment?
        }
    }
}
