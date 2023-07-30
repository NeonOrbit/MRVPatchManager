package app.neonorbit.mrvpatchmanager

import android.content.ContentResolver
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.net.Uri
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
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

    fun getAppCacheSize() = application.cacheDir.size()

    fun clearAppCache() = application.cacheDir.deleteRecursively()

    fun getCacheDir(): File = application.cacheDir

    val appFilesDir: File get() = application.filesDir.init()

    fun getFilesDir(sub: String): File {
        return File(application.filesDir, sub).init()
    }

    fun getCacheDir(sub: String): File {
        return File(application.cacheDir, sub).init()
    }

    private fun File.init() = apply { if (!exists()) mkdirs() }

    fun showToast(message: String, long: Boolean = false) {
        Toast.makeText(application, message,
            if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        ).show()
    }

    fun resolveDocumentTree(uri: Uri) = DocumentFile.fromTreeUri(application, uri)

    fun resolveContentUri(file: File): Uri? {
        return FileProvider.getUriForFile(application, AppConfig.FILE_PROVIDER_AUTH, file)
    }
}
