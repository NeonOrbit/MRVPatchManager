package app.neonorbit.mrvpatchmanager

import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.annotation.WorkerThread
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.io.File

object AppServices {
    private val application: App get() = App.instance

    val globalScope: CoroutineScope = CoroutineScope(SupervisorJob())

    val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(application)
    }

    val packageManager: PackageManager by lazy { application.packageManager }

    @WorkerThread
    fun isNetworkOnline() = SystemServices.Network.isOnline(application)

    fun getCacheDir(): File = application.cacheDir
}
