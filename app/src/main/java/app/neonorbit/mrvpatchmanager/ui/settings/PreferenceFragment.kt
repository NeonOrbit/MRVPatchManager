package app.neonorbit.mrvpatchmanager.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import app.neonorbit.mrvpatchmanager.util.AppUtil
import app.neonorbit.mrvpatchmanager.R
import app.neonorbit.mrvpatchmanager.toSize
import app.neonorbit.mrvpatchmanager.withLifecycle
import rikka.preference.SimpleMenuPreference

class PreferenceFragment : PreferenceFragmentCompat() {
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
        viewModel.cacheSize.observe(viewLifecycleOwner) { size ->
            findPreference<Preference>(KEY_PREF_CLEAR_CACHE)?.let {
                it.summary = size.toSize(true)
            }
        }
        viewModel.loadCacheSize()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference)

        findPreference<SimpleMenuPreference>(KEY_PREF_APK_SERVER)?.let { pref ->
            pref.entries = viewModel.servers
            pref.entryValues = viewModel.servers
            pref.summary = pref.value ?: viewModel.defaultServer.also {
                pref.value = it
            }
            pref.setOnPreferenceChangeListener { _, value ->
                pref.summary = value as CharSequence?
                true
            }
        }

        onSwitchChange(KEY_PREF_FALLBACK_MODE) { pref, value ->
            if (value) {
                AppUtil.prompt(requireContext(),
                    "Warning!",
                    "Do you really want to enable fallback mode?\n" +
                        "[Fallback patched apps are little slower]",
                    "Enable"
                ) {
                    pref.isChecked = it
                }
            }
            !value
        }

        onPreferenceClick(KEY_PREF_CLEAR_CACHE) {
            AppUtil.prompt(requireContext(),
                "Clear cache?", "This won't delete your patched apps.", "Clear"
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

    companion object {
        const val KEY_PREF_APK_SERVER = "pref_apk_server"
        const val KEY_PREF_FALLBACK_MODE = "pref_fallback_mode"
        const val KEY_PREF_MASK_PACKAGE = "pref_mask_package"
        const val KEY_PREF_CLEAR_CACHE = "pref_clear_cache"
        const val KEY_PREF_INSTRUCTION = "pref_instruction"
        const val KEY_PREF_TROUBLESHOOT = "pref_troubleshoot"
        const val KEY_PREF_SAFETY_NOTE = "pref_safety_note"
        const val KEY_PREF_VISIT_HELP = "pref_visit_help"
        const val KEY_PREF_VISIT_SOURCE = "pref_visit_source"
    }
}
