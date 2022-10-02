package app.neonorbit.mrvpatchmanager

import android.content.ContentResolver
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.AssetManager
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

    val assetManager: AssetManager get() = application.assets

    val packageManager: PackageManager by lazy { application.packageManager }

    val contentResolver: ContentResolver by lazy { application.contentResolver }

    @WorkerThread
    fun isNetworkOnline() = SystemServices.Network.isOnline(application)

    fun getCacheDir(): File = application.cacheDir

    fun getCacheDir(sub: String): File {
        return File(application.cacheDir, sub).also {
            if (!it.exists()) it.mkdirs()
        }
    }

    fun getFilesDir(sub: String): File {
        return File(application.filesDir, sub).also {
            if (!it.exists()) it.mkdirs()
        }
    }
}
