package app.neonorbit.mrvpatchmanager.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neonorbit.mrvpatchmanager.AppConfigs
import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.DefaultPreference
import app.neonorbit.mrvpatchmanager.error
import app.neonorbit.mrvpatchmanager.event.SingleEvent
import app.neonorbit.mrvpatchmanager.keystore.KeystoreData
import app.neonorbit.mrvpatchmanager.keystore.KeystoreInputData
import app.neonorbit.mrvpatchmanager.keystore.KeystoreManager
import app.neonorbit.mrvpatchmanager.post
import app.neonorbit.mrvpatchmanager.toTempFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel : ViewModel() {
    val uriEvent = SingleEvent<Uri>()
    val cacheSize = MutableStateFlow<Long?>(null)
    val keystoreName = MutableStateFlow<String?>(null)

    val ksSaveFailed = SingleEvent<String>()
    val keystoreSaved = SingleEvent<KeystoreData?>()

    private var cacheJob: Job? = null

    fun loadCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            cacheSize.emit(AppServices.getAppCacheSize())
        }
    }

    fun clearCache() {
        if (cacheJob != null) return
        viewModelScope.launch(Dispatchers.IO) {
            AppServices.clearAppCache()
            DefaultPreference.clearCache()
            withContext(Dispatchers.Main) {
                AppServices.showToast("Cache cleared")
            }
        }.also { cacheJob = it }.invokeOnCompletion {
            loadCacheSize()
            cacheJob = null
        }
    }

    fun loadKeystoreName() {
        viewModelScope.launch(Dispatchers.IO) {
            DefaultPreference.getCustomKeystore()?.let {
                keystoreName.emit(it.aliasName)
            }
        }
    }

    fun saveKeystore(input: KeystoreInputData?) {
        if (input == null) {
            SettingsData.CUSTOM_KEY_FILE.delete()
            keystoreName.post(with = this, null)
            keystoreSaved.post(viewModelScope, null)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            var keyfile: File? = null
            try {
                keyfile = input.uri.toTempFile(AppServices.contentResolver)
                KeystoreManager.readKeyData(
                    keyfile, SettingsData.CUSTOM_KEY_FILE.absolutePath,
                    input.password, input.aliasName, input.aliasPassword
                ).let { data ->
                    keyfile.copyTo(File(data.path), true)
                    keystoreName.emit(data.aliasName)
                    keystoreSaved.post(data)
                }
            } catch (e: Exception) {
                ksSaveFailed.post(viewModelScope, e.error)
            } finally {
                keyfile?.delete()
            }
        }
    }

    fun visitHelp() {
        uriEvent.post(viewModelScope, Uri.parse(AppConfigs.HELP_FORUM_URL))
    }

    fun visitGithub() {
        uriEvent.post(viewModelScope, Uri.parse(AppConfigs.GITHUB_REPO_URL))
    }
}
