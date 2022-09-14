package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.network.RetrofitClient
import app.neonorbit.mrvpatchmanager.result

object ApkComboService : ApkRemoteService {
    private const val BASE_URL = "https://www.apkcombo.com"
    private const val FILE_URL = "download/apk"
    private const val TOKEN_URL = "$BASE_URL/checkin"
    private const val FB_APP_URL = "$BASE_URL/facebook/com.facebook.katana/$FILE_URL"
    private const val FB_LITE_URL = "$BASE_URL/facebook-lite/com.facebook.lite/$FILE_URL"
    private const val MSG_APP_URL = "$BASE_URL/facebook-messenger/com.facebook.orca/$FILE_URL"
    private const val MSG_LITE_URL = "$BASE_URL/messenger-lite/com.facebook.mlite/$FILE_URL"
    private const val BSN_SUITE_URL = "$BASE_URL/meta-business-suite/com.facebook.pages.app/$FILE_URL"

    override fun server(): String {
        return "apkcombo.com"
    }

    override suspend fun fetchLink(type: AppType): String {
        return when (type) {
            AppType.FACEBOOK -> fetchDirectLink(FB_APP_URL)
            AppType.MESSENGER -> fetchDirectLink(MSG_APP_URL)
            AppType.FACEBOOK_LITE -> fetchDirectLink(FB_LITE_URL)
            AppType.MESSENGER_LITE -> fetchDirectLink(MSG_LITE_URL)
            AppType.BUSINESS_SUITE -> fetchDirectLink(BSN_SUITE_URL)
        }
    }

    private suspend fun fetchDirectLink(from: String): String {
        val service = RetrofitClient.SERVICE
        return service.get(TOKEN_URL).result().string().let { token ->
            service.getApkComboVariant(from).result().let { combo ->
                combo.variants.firstOrNull {
                    it.arch.contains("arm64-v8a")
                }?.apks?.firstOrNull {
                    it.type.equals("apk", true) && it.info.contains("nodpi")
                }?.let { apk ->
                    "${apk.link}&$token"
                } ?: "${combo.fallback.apks[0].link}&$token"
            }
        }
    }
}
