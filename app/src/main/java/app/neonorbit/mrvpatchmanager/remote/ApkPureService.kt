package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.network.HttpSpec
import app.neonorbit.mrvpatchmanager.network.RetrofitClient
import app.neonorbit.mrvpatchmanager.remote.data.RemoteApkInfo
import app.neonorbit.mrvpatchmanager.result

object ApkPureService : ApkRemoteService {
    private const val BASE_URL = "https://www.apkpure.com"
    private const val FILE_URL = "download?from=details"
    private const val FB_APP_URL = "$BASE_URL/facebook/com.facebook.katana/$FILE_URL"
    private const val FB_LITE_URL = "$BASE_URL/facebook-lite/com.facebook.lite/$FILE_URL"
    private const val MSG_APP_URL = "$BASE_URL/facebook-messenger/com.facebook.orca/$FILE_URL"
    private const val MSG_LITE_URL = "$BASE_URL/messenger-lite/com.facebook.mlite/$FILE_URL"
    private const val BSN_SUITE_URL = "$BASE_URL/meta-business-suite/com.facebook.pages.app/$FILE_URL"

    override fun server(): String {
        return "apkpure.com"
    }

    override suspend fun fetch(type: AppType): RemoteApkInfo {
        return when (type) {
            AppType.FACEBOOK -> fetchInfo(FB_APP_URL) ?: hardcodedInfo("katana")
            AppType.MESSENGER -> fetchInfo(MSG_APP_URL) ?: hardcodedInfo("orca")
            AppType.FACEBOOK_LITE -> fetchInfo(FB_LITE_URL) ?: hardcodedInfo("lite")
            AppType.MESSENGER_LITE -> fetchInfo(MSG_LITE_URL) ?: hardcodedInfo("mlite")
            AppType.BUSINESS_SUITE -> fetchInfo(BSN_SUITE_URL) ?: hardcodedInfo("pages.app")
        }
    }

    private suspend fun fetchInfo(from: String): RemoteApkInfo? {
        return try {
            RetrofitClient.SERVICE.getApkPureItemLink(from).result().let {
                RemoteApkInfo(it.link, it.versionName)
            }
        } catch (_: Exception) { null }
    }

    private suspend fun hardcodedInfo(id: String): RemoteApkInfo {
        val link = "https://d.apkpure.com/b/APK/com.facebook.$id?version=latest"
        return try {
            RetrofitClient.SERVICE.head(link).let {
                if (ApkUtil.APK_MIME_TYPE != it.headers()[HttpSpec.Header.CONTENT_TYPE]) {
                    throw Exception()
                }
            }
            RemoteApkInfo(link)
        } catch (_: Exception) {
            throw Exception("Failed to fetch apk info from server")
        }
    }
}
