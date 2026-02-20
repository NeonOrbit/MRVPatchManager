package app.neonorbit.mrvpatchmanager

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import app.neonorbit.mrvpatchmanager.util.Utils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

object AppServices {
    val application: MRVPatchManager get() = MRVPatchManager.instance

    val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(application)
    }

    val cachePreferences: SharedPreferences by lazy {
        application.getSharedPreferences("${application.packageName}_cache", Context.MODE_PRIVATE)
    }

    val assetManager: AssetManager get() = application.assets

    val packageManager: PackageManager by lazy { application.packageManager }

    val contentResolver: ContentResolver by lazy { application.contentResolver }

    val globalScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
            Utils.error("Global Coroutine Exception: ${throwable.message}", throwable)
        })
    }

    @WorkerThread
    fun isNetworkOnline() = SystemServices.Network.isOnline(application)

    fun clearTempDir() = AppConfigs.TEMP_DIR.deleteRecursively()

    fun clearAppCache() = application.cacheDir.deleteRecursively()

    fun getAppCacheSize() = application.cacheDir.totalSize()

    fun getCacheDir(): File = application.cacheDir

    val appFilesDir: File get() = application.filesDir.init()

    fun getFilesDir(sub: String): File {
        return File(application.filesDir, sub).init()
    }

    fun getCacheDir(sub: String): File {
        return File(application.cacheDir, sub).init()
    }

    private fun File.init() = apply { if (!exists()) mkdirs() }

    fun showToast(@StringRes resId: Int, long: Boolean = false) {
        showToast(application.getString(resId), long)
    }

    fun showToast(message: String, long: Boolean = false) {
        globalScope.launch(Dispatchers.Main) {
            val duration = if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            try {
                Toast.makeText(application, message, duration).show()
            } catch (_: Exception) {}
        }
    }

    fun resolveDocumentTree(uri: Uri) = DocumentFile.fromTreeUri(application, uri)

    fun resolveContentUri(file: File): Uri? {
        return FileProvider.getUriForFile(application, AppConfigs.FILE_PROVIDER_AUTH, file)
    }
}
