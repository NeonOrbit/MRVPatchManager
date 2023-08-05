package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.isConnectError
import app.neonorbit.mrvpatchmanager.network.RetrofitClient
import app.neonorbit.mrvpatchmanager.remote.data.ApkComboReleaseData
import app.neonorbit.mrvpatchmanager.remote.data.RemoteApkInfo
import app.neonorbit.mrvpatchmanager.result
import app.neonorbit.mrvpatchmanager.util.Utils
import app.neonorbit.mrvpatchmanager.util.Utils.LOG
import kotlin.coroutines.cancellation.CancellationException

object ApkComboService : ApkRemoteService {
    const val BASE_URL = "https://www.apkcombo.com"
    private const val RELEASE_URL = "old-versions"
    private const val TOKEN_URL = "$BASE_URL/checkin"
    private const val FB_APP_URL = "$BASE_URL/facebook/com.facebook.katana/$RELEASE_URL"
    private const val FB_LITE_URL = "$BASE_URL/facebook-lite/com.facebook.lite/$RELEASE_URL"
    private const val MSG_APP_URL = "$BASE_URL/facebook-messenger/com.facebook.orca/$RELEASE_URL"
    private const val MSG_LITE_URL = "$BASE_URL/messenger-lite/com.facebook.mlite/$RELEASE_URL"
    private const val BSN_SUITE_URL = "$BASE_URL/meta-business-suite/com.facebook.pages.app/$RELEASE_URL"

    override fun server(): String {
        return "apkcombo.com"
    }

    override suspend fun fetch(type: AppType, abi: String): RemoteApkInfo {
        return try {
            when (type) {
                AppType.FACEBOOK -> fetchInfo(FB_APP_URL, abi)
                AppType.MESSENGER -> fetchInfo(MSG_APP_URL, abi)
                AppType.FACEBOOK_LITE -> fetchInfo(FB_LITE_URL, abi)
                AppType.MESSENGER_LITE -> fetchInfo(MSG_LITE_URL, abi)
                AppType.BUSINESS_SUITE -> fetchInfo(BSN_SUITE_URL, abi)
            }
        } catch (e: Exception) {
            throw if (e is CancellationException || e.isConnectError) e else {
                e.message?.let { Utils.warn(it, e) }
                throw Exception("Failed to fetch apk info from the server: ${server()}")
            }
        }
    }

    private suspend fun fetchInfo(from: String, abi: String): RemoteApkInfo {
        val service = RetrofitClient.SERVICE
        return service.get(TOKEN_URL).result().string().let { token ->
            service.getApkComboRelease(from).result().releases.LOG("Releases").filter {
                it.isValidType && ApkConfigs.isValidRelease(it.name)
            }.take(3).LOG("Filtered").selectApk(abi).LOG("Selected")?.let { apk ->
                RemoteApkInfo("${apk.link}&$token", apk.versionName)
            }
        } ?: throw Exception()
    }

    private suspend fun List<ApkComboReleaseData.Release>.selectApk(abi: String) = firstNotNullOfOrNull { release ->
        RetrofitClient.SERVICE.getApkComboVariant(release.link).result().let { combo ->
            combo.variants.LOG("Variants").firstOrNull {
                it.arch.lowercase().contains(abi)
            }.LOG("Filtered")?.apks.LOG("APKs")?.filter {
                it.isValidType && ApkConfigs.isValidDPI(it.info) && ApkConfigs.isSupportedMinVersion(it.info)
            }.LOG("Filtered")?.let { filtered ->
                filtered.firstOrNull { ApkConfigs.isPreferredDPI(it.info) } ?: filtered.firstOrNull()
            } ?: combo.fallback?.takeIf { it.isValidType }.LOG("Fallback")
        }
    }
}
