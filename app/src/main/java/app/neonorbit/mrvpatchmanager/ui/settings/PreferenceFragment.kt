package app.neonorbit.mrvpatchmanager.ui.settings

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceFragmentCompat
import app.neonorbit.mrvpatchmanager.R

class PreferenceFragment : PreferenceFragmentCompat() {
    private var _viewModel: SettingsViewModel? = null
    private val viewModel: SettingsViewModel get() = _viewModel!!

    override fun onCreate(savedInstanceState: Bundle?) {
        _viewModel = viewModels<SettingsViewModel>(
            { requireParentFragment() }
        ).value
        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference)
    }

    override fun onDestroy() {
        super.onDestroy()
        _viewModel = null
    }
}
