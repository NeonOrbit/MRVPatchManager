package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.network.RetrofitClient
import app.neonorbit.mrvpatchmanager.remote.data.ApkFlashReleaseData
import app.neonorbit.mrvpatchmanager.remote.data.RemoteApkInfo
import app.neonorbit.mrvpatchmanager.result
import app.neonorbit.mrvpatchmanager.util.Utils.LOG

object ApkFlashService : ApkRemoteService {
    const val BASE_URL = "https://apkflash.com"
    private const val APK_URL = "$BASE_URL/apk/app"
    private const val RELEASE_URL = "old-versions"
    private const val TOKEN_URL = "$BASE_URL/checkin/"
    private const val FB_APP_URL = "$APK_URL/com.facebook.katana/facebook/$RELEASE_URL"
    private const val FB_LITE_URL = "$APK_URL/com.facebook.lite/facebook-lite/$RELEASE_URL"
    private const val MSG_APP_URL = "$APK_URL/com.facebook.orca/facebook-messenger/$RELEASE_URL"
    private const val MSG_LITE_URL = "$APK_URL/com.facebook.mlite/messenger-lite/$RELEASE_URL"
    private const val BSN_SUITE_URL = "$APK_URL/com.facebook.pages.app/meta-business-suite/$RELEASE_URL"

    override fun server(): String {
        return "apkflash.com"
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
            RetrofitClient.SERVICE.getApkFlashRelease(from).result().LOG("Response").releases.LOG("Releases").filter {
                it.isValidType && ApkConfigs.isValidRelease(it.name) && ApkConfigs.matchApkVersion(it.version, ver)
            }.LOG("Filtered").sortedWith(
                ApkConfigs.compareLatest({ it.version })
            ).LOG("Sorted").take(5).selectApk(abi).LOG("Selected")?.let { apk ->
                RemoteApkInfo("${apk.link}&$token", apk.version)
            }
        } ?: throw Exception()
    }

    private suspend fun  List<ApkFlashReleaseData.Release>.selectApk(abi: String) = firstNotNullOfOrNull { release ->
        RetrofitClient.SERVICE.getApkFlashVariant(release.link).result().LOG("Response").let { data ->
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
