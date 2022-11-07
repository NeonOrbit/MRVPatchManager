package app.neonorbit.mrvpatchmanager.ui.settings

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neonorbit.mrvpatchmanager.AppConfig
import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.event.SingleEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel : ViewModel() {
    val defaultServer = SettingsData.DEFAULT_SERVER
    val servers: Array<String> get() = SettingsData.SERVERS

    val uriEvent = SingleEvent<Uri>()
    val cacheSize = MutableLiveData<Long>()

    private var cacheJob: Job? = null

    fun loadCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            cacheSize.postValue(AppServices.getAppCacheSize())
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

    fun visitHelp() {
        uriEvent.post(viewModelScope, Uri.parse(AppConfig.HELP_FORUM_URL))
    }

    fun visitGithub() {
        uriEvent.post(viewModelScope, Uri.parse(AppConfig.GITHUB_REPO_URL))
    }
}
