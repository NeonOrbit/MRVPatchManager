package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.network.RetrofitClient
import app.neonorbit.mrvpatchmanager.remote.data.ApkComboReleaseData
import app.neonorbit.mrvpatchmanager.remote.data.RemoteApkInfo
import app.neonorbit.mrvpatchmanager.result
import app.neonorbit.mrvpatchmanager.util.Utils.LOG

object ApkComboService : ApkRemoteService {
    const val BASE_URL = "https://apkcombo.com"
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

    override suspend fun fetch(type: AppType, abi: String, ver: String?): RemoteApkInfo {
        return try {
            when (type) {
                AppType.FACEBOOK -> fetchInfo(FB_APP_URL, abi, ver)
                AppType.MESSENGER -> fetchInfo(MSG_APP_URL, abi, ver)
                AppType.FACEBOOK_LITE -> fetchInfo(FB_LITE_URL, abi, ver)
                AppType.MESSENGER_LITE -> fetchInfo(MSG_LITE_URL, abi, ver)
                AppType.BUSINESS_SUITE -> fetchInfo(BSN_SUITE_URL, abi, ver)
            }
        } catch (exception: Exception) {
            exception.handleApkServiceException(type, ver)
        }
    }

    private suspend fun fetchInfo(from: String, abi: String, ver: String?): RemoteApkInfo {
        return RetrofitClient.SERVICE.get(TOKEN_URL).result().string().LOG("Token").let { token ->
            RetrofitClient.SERVICE.getApkComboRelease(from).result().LOG("Response").releases.LOG("Releases").filter {
                it.isValidType && ApkConfigs.isValidRelease(it.name) && ApkConfigs.matchApkVersion(it.version, ver)
            }.LOG("Filtered").sortedWith(
                ApkConfigs.compareLatest({ it.version })
            ).LOG("Sorted").take(5).selectApk(abi).LOG("Selected")?.let { apk ->
                RemoteApkInfo("${apk.link}&$token", apk.version)
            }
        } ?: throw Exception()
    }

    private suspend fun List<ApkComboReleaseData.Release>.selectApk(abi: String) = firstNotNullOfOrNull { release ->
        RetrofitClient.SERVICE.getApkComboVariant(release.link).result().LOG("Response").let { data ->
            data.variants.LOG("Variants").firstOrNull {
                it.arch.lowercase().contains(abi)
            }.LOG("Filtered")?.apks.LOG("APKs")?.filter {
                it.isValidType && ApkConfigs.isSupportedDPI(it.dpi) && ApkConfigs.isSupportedMinSdk(it.minSDk)
            }.LOG("Filtered")?.let { filtered ->
                filtered.firstOrNull { ApkConfigs.isPreferredDPI(it.dpi) } ?: filtered.firstOrNull()
            }
        }
    }
}
