package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.isConnectError
import app.neonorbit.mrvpatchmanager.network.HttpSpec
import app.neonorbit.mrvpatchmanager.network.RetrofitClient
import app.neonorbit.mrvpatchmanager.remote.data.ApkPureReleaseData
import app.neonorbit.mrvpatchmanager.remote.data.RemoteApkInfo
import app.neonorbit.mrvpatchmanager.result
import app.neonorbit.mrvpatchmanager.util.Utils
import app.neonorbit.mrvpatchmanager.util.Utils.LOG
import kotlin.coroutines.cancellation.CancellationException

object ApkPureService : ApkRemoteService {
    const val BASE_URL = "https://apkpure.com"
    private const val RELEASE_URL = "versions"
    private const val FB_APP_URL = "$BASE_URL/facebook/com.facebook.katana/$RELEASE_URL"
    private const val FB_LITE_URL = "$BASE_URL/facebook-lite/com.facebook.lite/$RELEASE_URL"
    private const val MSG_APP_URL = "$BASE_URL/facebook-messenger/com.facebook.orca/$RELEASE_URL"
    private const val MSG_LITE_URL = "$BASE_URL/messenger-lite/com.facebook.mlite/$RELEASE_URL"
    private const val BSN_SUITE_URL = "$BASE_URL/meta-business-suite/com.facebook.pages.app/$RELEASE_URL"

    override fun server(): String {
        return "apkpure.com"
    }

    override suspend fun fetch(type: AppType, abi: String, ver: String?): RemoteApkInfo {
        return when (type) {
            AppType.FACEBOOK -> fetchInfo(type, FB_APP_URL, abi, ver) ?: hardcodedInfo("katana")
            AppType.MESSENGER -> fetchInfo(type, MSG_APP_URL, abi, ver) ?: hardcodedInfo("orca")
            AppType.FACEBOOK_LITE -> fetchInfo(type, FB_LITE_URL, abi, ver) ?: hardcodedInfo("lite")
            AppType.MESSENGER_LITE -> fetchInfo(type, MSG_LITE_URL, abi, ver) ?: hardcodedInfo("mlite")
            AppType.BUSINESS_SUITE -> fetchInfo(type, BSN_SUITE_URL, abi, ver) ?: hardcodedInfo("pages.app")
        }
    }

    private suspend fun fetchInfo(type: AppType, from: String, abi: String, ver: String?): RemoteApkInfo? {
        return try {
            RetrofitClient.SERVICE.getApkPureRelease(from).result().LOG("Response").releases.LOG("Releases").filter {
                it.isValidType && ApkConfigs.isValidRelease(it.name) && ApkConfigs.matchApkVersion(it.version, ver)
            }.LOG("Filtered").sortedWith(
                ApkConfigs.compareLatest({ it.version })
            ).LOG("Sorted").take(5).selectApk(abi).LOG("Selected")?.let {
                RemoteApkInfo(it.link, it.version)
            } ?: throw Exception()
        } catch (exception: Exception) {
            exception.handleApkServiceException(type, ver, ver != null || abi != ApkConfigs.ARM_64)
            Utils.warn("Falling back: ${server()}", exception)
            null
        }
    }

    private suspend fun  List<ApkPureReleaseData.Release>.selectApk(abi: String) = firstNotNullOfOrNull { release ->
        RetrofitClient.SERVICE.getApkPureVariant(release.link).result().LOG("Response").let { data ->
            data.variants.LOG("Variants").firstOrNull {
                it.arch.lowercase().contains(abi)
            }.LOG("Filtered")?.apks.LOG("APKs")?.firstOrNull {
                it.isValidType && ApkConfigs.isSupportedMinSdk(it.minSDk)
            }
        }
    }

    private suspend fun hardcodedInfo(id: String): RemoteApkInfo {
        val link = "https://d.apkpure.com/b/APK/com.facebook.$id?version=latest"
        return try {
            RetrofitClient.SERVICE.head(link).let {
                if (ApkConfigs.APK_MIME_TYPE != it.headers()[HttpSpec.Header.CONTENT_TYPE]) {
                    throw Exception()
                }
            }
            RemoteApkInfo(link)
        } catch (e: Exception) {
            throw if (e is CancellationException || e.isConnectError) e else Exception(
                "Failed to fetch apk info from the server ${server()}"
            )
        }
    }
}
