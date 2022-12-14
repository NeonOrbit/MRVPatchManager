package app.neonorbit.mrvpatchmanager

import android.content.SharedPreferences
import app.neonorbit.mrvpatchmanager.ui.settings.PreferenceFragment

@Suppress("unused")
object DefaultPreference {
    private val preferences: SharedPreferences get() = AppServices.preferences

    private const val KEY_PREF_APK_SERVER = PreferenceFragment.KEY_PREF_APK_SERVER
    private const val KEY_PREF_FALLBACK_MODE = PreferenceFragment.KEY_PREF_FALLBACK_MODE
    private const val KEY_PREF_MASK_PACKAGE = PreferenceFragment.KEY_PREF_MASK_PACKAGE

    fun getApkServer(): String? = getString(KEY_PREF_APK_SERVER)

    fun isFallbackMode(): Boolean = getBoolean(KEY_PREF_FALLBACK_MODE)

    fun isPackageMaskEnabled(): Boolean = getBoolean(KEY_PREF_MASK_PACKAGE)

    fun getString(key: String): String? {
        return preferences.getString(key, null)
    }

    fun setString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    private fun getBoolean(key: String): Boolean {
        return preferences.getBoolean(key, false)
    }

    private fun setBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    private fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}
