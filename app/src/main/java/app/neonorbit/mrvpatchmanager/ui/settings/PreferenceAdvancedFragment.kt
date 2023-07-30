package app.neonorbit.mrvpatchmanager.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.DefaultPreference
import app.neonorbit.mrvpatchmanager.R
import app.neonorbit.mrvpatchmanager.isValidJavaName
import app.neonorbit.mrvpatchmanager.keystore.KeystoreInputData
import app.neonorbit.mrvpatchmanager.observeOnUI
import app.neonorbit.mrvpatchmanager.util.AppUtil
import app.neonorbit.mrvpatchmanager.withLifecycle

class PreferenceAdvancedFragment : PreferenceFragmentCompat(), KeystoreDialogFragment.ResponseListener {
    private var _viewModel: SettingsViewModel? = null
    private val viewModel: SettingsViewModel get() = _viewModel!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewModel = viewModels<SettingsViewModel>(
            { requireParentFragment() }
        ).value
        viewModel.keystoreName.observeOnUI(viewLifecycleOwner) { keyName ->
            findPreference<Preference>(KEY_PREF_CUSTOM_KEYSTORE)?.let {
                it.summary = if (keyName == null) getString(R.string.pref_custom_keystore_summery)
                else getString(R.string.pref_custom_keystore_keystore, keyName)
            }
        }
        viewLifecycleOwner.withLifecycle(Lifecycle.State.STARTED) {
            viewModel.keystoreSaved.observe { data ->
                DefaultPreference.setString(KEY_PREF_CUSTOM_KEYSTORE, data?.toJson())
                KeystoreDialogFragment.finish(this@PreferenceAdvancedFragment)
                AppServices.showToast(getString(
                    if (data != null) R.string.text_saved else R.string.text_cleared
                ))
            }
        }
        viewLifecycleOwner.withLifecycle(Lifecycle.State.STARTED) {
            viewModel.ksSaveFailed.observe {
                KeystoreDialogFragment.failed(this@PreferenceAdvancedFragment, it)
            }
        }
        viewModel.loadKeystoreName()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_advanced, rootKey)

        onPreferenceClick(KEY_PREF_CUSTOM_KEYSTORE) {
            KeystoreDialogFragment.show(this)
        }

        findPreference<EditTextPreference>(KEY_PREF_EXTRA_MODULES)?.let { pref ->
            if (pref.text?.isNotEmpty() == true) {
                pref.summary = getString(R.string.pref_extra_modules_package, pref.text)
            }
            pref.setOnPreferenceChangeListener { _, value ->
                val packages = value.toString().split(',').map { it.trim() }.filter { it.isNotEmpty() }
                for (pkg in packages) {
                    if (!pkg.isValidJavaName()) {
                        AppUtil.prompt(requireContext(), R.string.pref_extra_modules_invalid_pkg, pkg)
                        return@setOnPreferenceChangeListener false
                    }
                }
                pref.text = packages.joinToString(", ")
                pref.summary = if (packages.isEmpty()) getString(R.string.pref_extra_modules_summery)
                else getString(R.string.pref_extra_modules_package, pref.text)
                false
            }
        }

        onPreferenceClick(KEY_PREF_ADVANCED_BACK) {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onKeystoreInput(response: KeystoreInputData?) {
        viewModel.saveKeystore(response)
    }

    override fun onDestroy() {
        super.onDestroy()
        _viewModel = null
    }

    @Suppress("SameParameterValue")
    private fun onPreferenceClick(key: String, block: () -> Unit) {
        findPreference<Preference>(key)?.let { pref ->
            pref.setOnPreferenceClickListener {
                block()
                true
            }
        }
    }

    companion object {
        const val KEY_PREF_ADVANCED_BACK = "pref_advanced_back"
        const val KEY_PREF_EXTRA_MODULES = "pref_extra_modules"
        const val KEY_PREF_CUSTOM_KEYSTORE = "pref_custom_keystore"
    }
}
