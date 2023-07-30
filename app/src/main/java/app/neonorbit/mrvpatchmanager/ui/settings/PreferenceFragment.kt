package app.neonorbit.mrvpatchmanager.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.DefaultPreference
import app.neonorbit.mrvpatchmanager.R
import app.neonorbit.mrvpatchmanager.isValidJavaName
import app.neonorbit.mrvpatchmanager.keystore.KeystoreInputData
import app.neonorbit.mrvpatchmanager.observeOnUI
import app.neonorbit.mrvpatchmanager.toSize
import app.neonorbit.mrvpatchmanager.util.AppUtil
import app.neonorbit.mrvpatchmanager.withLifecycle
import rikka.preference.SimpleMenuPreference

class PreferenceFragment : PreferenceFragmentCompat(), KeystoreDialogFragment.ResponseListener {
    private var _viewModel: SettingsViewModel? = null
    private val viewModel: SettingsViewModel get() = _viewModel!!

    private val uriLauncher by lazy {
        CustomTabsIntent.Builder().build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        _viewModel = viewModels<SettingsViewModel>(
            { requireParentFragment() }
        ).value
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewLifecycleOwner.withLifecycle(Lifecycle.State.STARTED) {
            viewModel.uriEvent.observe {
                uriLauncher.launchUrl(requireContext(), it)
            }
        }
        viewModel.cacheSize.observeOnUI(viewLifecycleOwner) { size ->
            findPreference<Preference>(KEY_PREF_CLEAR_CACHE)?.let {
                it.summary = size?.toSize(true) ?: getString(R.string.pref_clear_cache_summery)
            }
        }
        viewModel.keystoreName.observeOnUI(viewLifecycleOwner) { keyName ->
            findPreference<Preference>(KEY_PREF_CUSTOM_KEYSTORE)?.let {
                it.summary = if (keyName == null) getString(R.string.pref_custom_keystore_summery)
                else getString(R.string.pref_custom_keystore_keystore, keyName)
            }
        }
        viewLifecycleOwner.withLifecycle(Lifecycle.State.STARTED) {
            viewModel.keystoreSaved.observe { data ->
                DefaultPreference.setString(KEY_PREF_CUSTOM_KEYSTORE, data?.toJson())
                KeystoreDialogFragment.finish(this@PreferenceFragment)
                AppServices.showToast(getString(
                    if (data != null) R.string.text_saved else R.string.text_cleared
                ))
            }
        }
        viewLifecycleOwner.withLifecycle(Lifecycle.State.STARTED) {
            viewModel.ksSaveFailed.observe {
                KeystoreDialogFragment.failed(this@PreferenceFragment, it)
            }
        }
        viewModel.loadCacheSize()
        viewModel.loadKeystoreName()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onKeystoreInput(response: KeystoreInputData?) {
        viewModel.saveKeystore(response)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference)

        findPreference<SimpleMenuPreference>(KEY_PREF_APK_SERVER)?.let { pref ->
            pref.entries = SettingsData.SERVERS
            pref.entryValues = SettingsData.SERVERS
            pref.summary = pref.value ?: SettingsData.DEFAULT_SERVER.also {
                pref.value = it
            }
            pref.setOnPreferenceChangeListener { _, value ->
                pref.summary = value as CharSequence?
                true
            }
        }

        setSwitchConfirmation(KEY_PREF_FIX_CONFLICT, R.string.text_warning, R.string.pref_fix_conflict_confirm_message)
        setSwitchConfirmation(KEY_PREF_MASK_PACKAGE, R.string.text_warning, R.string.pref_mask_package_confirm_message)
        setSwitchConfirmation(KEY_PREF_FALLBACK_MODE, R.string.text_warning, R.string.pref_fallback_mode_confirm_message)

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

        onPreferenceClick(KEY_PREF_CUSTOM_KEYSTORE) {
            KeystoreDialogFragment.show(this)
        }

        onPreferenceClick(KEY_PREF_CLEAR_CACHE) {
            AppUtil.prompt(requireContext(),
                R.string.pref_clear_cache_confirm_prompt, R.string.pref_clear_cache_confirm_message, R.string.text_clear
            ) {
                if (it) viewModel.clearCache()
            }
        }

        onPreferenceClick(KEY_PREF_INSTRUCTION) {
            AppUtil.show(requireContext(), R.string.instructions)
        }

        onPreferenceClick(KEY_PREF_TROUBLESHOOT) {
            AppUtil.show(requireContext(), R.string.troubleshoot)
        }

        onPreferenceClick(KEY_PREF_SAFETY_NOTE) {
            AppUtil.show(requireContext(), R.string.safety_note)
        }

        onPreferenceClick(KEY_PREF_VISIT_HELP) {
            viewModel.visitHelp()
        }

        onPreferenceClick(KEY_PREF_VISIT_SOURCE) {
            viewModel.visitGithub()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _viewModel = null
    }

    private fun onPreferenceClick(key: String, block: () -> Unit) {
        findPreference<Preference>(key)?.let { pref ->
            pref.setOnPreferenceClickListener {
                block()
                true
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun onSwitchChange(key: String, block: (SwitchPreferenceCompat, Boolean) -> Boolean) {
        findPreference<SwitchPreferenceCompat>(key)?.let { pref ->
            pref.setOnPreferenceChangeListener { _, value ->
                block(pref, value as Boolean)
            }
        }
    }

    private fun setSwitchConfirmation(key: String,
                                      @StringRes title: Int,
                                      @StringRes message: Int? = null,
                                      @StringRes positive: Int? = R.string.text_enable) {
        onSwitchChange(key) { pref, value ->
            if (value) {
                AppUtil.prompt(requireContext(), title, message, positive) {
                    pref.isChecked = it
                }
            }
            !value
        }
    }

    companion object {
        const val KEY_PREF_APK_SERVER = "pref_apk_server"
        const val KEY_PREF_FIX_CONFLICT = "pref_fix_conflict"
        const val KEY_PREF_MASK_PACKAGE = "pref_mask_package"
        const val KEY_PREF_FALLBACK_MODE = "pref_fallback_mode"
        const val KEY_PREF_EXTRA_MODULES = "pref_extra_modules"
        const val KEY_PREF_CUSTOM_KEYSTORE = "pref_custom_keystore"
        const val KEY_PREF_CLEAR_CACHE = "pref_clear_cache"
        const val KEY_PREF_INSTRUCTION = "pref_instruction"
        const val KEY_PREF_TROUBLESHOOT = "pref_troubleshoot"
        const val KEY_PREF_SAFETY_NOTE = "pref_safety_note"
        const val KEY_PREF_VISIT_HELP = "pref_visit_help"
        const val KEY_PREF_VISIT_SOURCE = "pref_visit_source"
    }
}
