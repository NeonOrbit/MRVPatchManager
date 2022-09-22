package app.neonorbit.mrvpatchmanager

import android.content.SharedPreferences

@Suppress("unused")
object DefaultPreference {
    private val preferences: SharedPreferences get() = AppServices.preferences

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
