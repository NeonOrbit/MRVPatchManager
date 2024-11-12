package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.AppConfigs
import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.CacheManager
import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import app.neonorbit.mrvpatchmanager.compareVersion
import app.neonorbit.mrvpatchmanager.data.UpdateEventData
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
        return fetchDirectLink(AppConfigs.MANAGER_PACKAGE, MANAGER_URL)
    }

    suspend fun getModuleLink(): String {
        return fetchDirectLink(AppConfigs.MODULE_PACKAGE, MODULE_URL)
    }

    fun checkForUpdate(force: Boolean = false) {
        AppServices.globalScope.launch(Dispatchers.IO) {
            EventBus.getDefault().removeStickyEvent(UpdateEventData.Manager::class.java)
            fetchUpdate(AppConfigs.MANAGER_PACKAGE, MANAGER_URL, force)?.let {
                EventBus.getDefault().postSticky(it)
            }
        }
        AppServices.globalScope.launch(Dispatchers.IO) {
            EventBus.getDefault().removeStickyEvent(UpdateEventData.Module::class.java)
            fetchUpdate(AppConfigs.MODULE_PACKAGE, MODULE_URL, force)?.let {
                EventBus.getDefault().postSticky(it)
            }
        }
    }

    private suspend fun fetchDirectLink(pkg: String, from: String): String {
        return fetchData(pkg, from, false)!!.assets[0].link
    }

    private suspend fun fetchUpdate(pkg: String, url: String, force: Boolean): UpdateEventData? {
        return ApkUtil.getPrefixedVersionName(pkg)?.let { current ->
            fetchData(pkg, url, force)?.takeIf {
                it.version.compareVersion(current) > 0
            }?.let {
                if (pkg == AppConfigs.MANAGER_PACKAGE)
                    UpdateEventData.Manager(current, it.version, it.assets[0].link)
                else UpdateEventData.Module(current, it.version, it.assets[0].link)
            }
        }
    }

    private suspend fun fetchData(pkg: String, url: String, force: Boolean): GithubReleaseData? {
        val cacheKey: String = getCacheKey(pkg)
        return (if (force) null else CacheManager.get(cacheKey)) ?: try {
            RetrofitClient.SERVICE.getGithubRelease(url).result().also {
                CacheManager.put(cacheKey, it, CACHE_TIMEOUT_HOUR)
            }
        } catch (_: Exception) {
            CacheManager.get(cacheKey, true)
        }
    }

    private fun getCacheKey(pkg: String): String {
        return "${KEY_UPDATE_CACHE}_${pkg.substringAfterLast('.')}"
    }
}
