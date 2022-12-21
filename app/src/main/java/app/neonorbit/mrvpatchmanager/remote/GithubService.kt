package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.AppConfig
import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.CacheManager
import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import app.neonorbit.mrvpatchmanager.compareVersion
import app.neonorbit.mrvpatchmanager.event.UpdateEvent
import app.neonorbit.mrvpatchmanager.network.RetrofitClient
import app.neonorbit.mrvpatchmanager.remote.data.GithubReleaseData
import app.neonorbit.mrvpatchmanager.result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

object GithubService {
    private const val CACHE_TIMEOUT_HOUR = 20
    private const val KEY_UPDATE_CACHE = "update_check_cache"
    private const val BASE_URL = "https://api.github.com/repos"
    private const val MANAGER_URL = "$BASE_URL/NeonOrbit/MRVPatchManager/releases/latest"
    private const val MODULE_URL = "$BASE_URL/NeonOrbit/ChatHeadEnabler/releases/latest"

    suspend fun getManagerLink(): String {
        return fetchDirectLink(AppConfig.MANAGER_PACKAGE, MANAGER_URL)
    }

    suspend fun getModuleLink(): String {
        return fetchDirectLink(AppConfig.MODULE_PACKAGE, MODULE_URL)
    }

    fun checkForUpdate() {
        AppServices.globalScope.launch(Dispatchers.IO) {
            EventBus.getDefault().removeStickyEvent(UpdateEvent.Manager::class.java)
            fetchUpdate(AppConfig.MANAGER_PACKAGE, MANAGER_URL)?.let {
                EventBus.getDefault().postSticky(it)
            }
        }
        AppServices.globalScope.launch(Dispatchers.IO) {
            EventBus.getDefault().removeStickyEvent(UpdateEvent.Module::class.java)
            fetchUpdate(AppConfig.MODULE_PACKAGE, MODULE_URL)?.let {
                EventBus.getDefault().postSticky(it)
            }
        }
    }

    private suspend fun fetchDirectLink(pkg: String, from: String): String {
        return fetchData(pkg, from)!!.assets[0].link
    }

    private suspend fun fetchUpdate(pkg: String, url: String): UpdateEvent? {
        return ApkUtil.getPrefixedVersionName(pkg)?.let { current ->
            fetchData(pkg, url)?.takeIf {
                it.version.compareVersion(current) > 0
            }?.let {
                if (pkg == AppConfig.MANAGER_PACKAGE)
                    UpdateEvent.Manager(current, it.version, it.assets[0].link)
                else UpdateEvent.Module(current, it.version, it.assets[0].link)
            }
        }
    }

    private suspend fun fetchData(pkg: String, url: String): GithubReleaseData? {
        val cacheKey = getKey(pkg)
        return CacheManager.get(cacheKey) ?: try {
            RetrofitClient.SERVICE.getGithubRelease(url).result().also {
                CacheManager.put(cacheKey, it, CACHE_TIMEOUT_HOUR)
            }
        } catch (_: Exception) {
            CacheManager.get(cacheKey, true)
        }
    }

    private fun getKey(pkg: String): String {
        return "${KEY_UPDATE_CACHE}_${pkg.substringAfterLast('.')}"
    }
}
