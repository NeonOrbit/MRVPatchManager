package app.neonorbit.mrvpatchmanager.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neonorbit.mrvpatchmanager.AppConfig
import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.DefaultPreference
import app.neonorbit.mrvpatchmanager.copyTo
import app.neonorbit.mrvpatchmanager.error
import app.neonorbit.mrvpatchmanager.event.SingleEvent
import app.neonorbit.mrvpatchmanager.keystore.KeystoreData
import app.neonorbit.mrvpatchmanager.keystore.KeystoreInputData
import app.neonorbit.mrvpatchmanager.keystore.KeystoreManager
import app.neonorbit.mrvpatchmanager.post
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            keystoreName.post(null, with = this)
            keystoreSaved.post(viewModelScope, null)
            SettingsData.CUSTOM_KEY_FILE.delete()
            return
        }
        viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, t ->
            SettingsData.CUSTOM_KEY_FILE.delete()
            ksSaveFailed.post(viewModelScope, t.error)
        }) {
            input.uri.copyTo(SettingsData.CUSTOM_KEY_FILE).let { file ->
                KeystoreManager.getVerifiedData(
                    file, input.password, input.aliasName, input.aliasPassword
                ).let { data ->
                    keystoreSaved.post(data)
                    keystoreName.emit(data.aliasName)
                }
            }
        }
    }

    fun visitHelp() {
        uriEvent.post(viewModelScope, Uri.parse(AppConfig.HELP_FORUM_URL))
    }

    fun visitGithub() {
        uriEvent.post(viewModelScope, Uri.parse(AppConfig.GITHUB_REPO_URL))
    }
}
