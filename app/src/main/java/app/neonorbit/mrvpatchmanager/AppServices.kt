package app.neonorbit.mrvpatchmanager

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import java.io.File

object AppServices {
    private val application: App get() = App.instance

    val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(application)
    }

    fun getCacheDir(): File = application.cacheDir
}
