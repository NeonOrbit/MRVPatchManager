package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.network.HttpSpec
import app.neonorbit.mrvpatchmanager.network.RetrofitClient
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

    override suspend fun fetchLink(type: AppType): String {
        return when (type) {
            AppType.FACEBOOK -> getDirectLink("katana") ?: fetchDirectLink(FB_APP_URL)
            AppType.MESSENGER -> getDirectLink("orca") ?: fetchDirectLink(MSG_APP_URL)
            AppType.FACEBOOK_LITE -> getDirectLink("lite") ?: fetchDirectLink(FB_LITE_URL)
            AppType.MESSENGER_LITE -> getDirectLink("mlite") ?: fetchDirectLink(MSG_LITE_URL)
            AppType.BUSINESS_SUITE -> getDirectLink("pages.app") ?: fetchDirectLink(BSN_SUITE_URL)
        }
    }

    private suspend fun getDirectLink(id: String): String? {
        val link = "https://d.apkpure.com/b/APK/com.facebook.$id?version=latest"
        return try {
            RetrofitClient.SERVICE.head(link).headers()[HttpSpec.Header.CONTENT_TYPE].let {
                if (ApkUtil.APK_MIME_TYPE == it) link else null
            }
        } catch (_: Exception) { null }
    }

    private suspend fun fetchDirectLink(from: String): String {
        return RetrofitClient.SERVICE.getApkPureItemLink(from).result().link
    }
}
