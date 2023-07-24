package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.network.RetrofitClient
import app.neonorbit.mrvpatchmanager.remote.data.RemoteApkInfo
import app.neonorbit.mrvpatchmanager.result

object ApkMirrorService : ApkRemoteService {
    const val BASE_URL = "https://www.apkmirror.com"
    private const val META_URL = "$BASE_URL/apk/facebook-2"
    private const val FEED_URL = "variant-{\"arches_slug\":[\"arm64-v8a\"],\"dpis_slug\":[\"nodpi\"]}/feed"
    private const val FB_APP_URL = "$META_URL/facebook/$FEED_URL"
    private const val FB_LITE_URL = "$META_URL/lite/$FEED_URL"
    private const val MSG_APP_URL = "$META_URL/messenger/$FEED_URL"
    private const val MSG_LITE_URL = "$META_URL/messenger-lite/$FEED_URL"
    private const val BSN_SUITE_URL = "$META_URL/pages-manager/$FEED_URL"

    override fun server(): String {
        return "apkmirror.com"
    }

    override suspend fun fetch(type: AppType): RemoteApkInfo {
        return when (type) {
            AppType.FACEBOOK -> fetchInfo(FB_APP_URL)
            AppType.MESSENGER -> fetchInfo(MSG_APP_URL)
            AppType.FACEBOOK_LITE -> fetchInfo(FB_LITE_URL)
            AppType.MESSENGER_LITE -> fetchInfo(MSG_LITE_URL)
            AppType.BUSINESS_SUITE -> fetchInfo(BSN_SUITE_URL)
        }
    }

    private suspend fun fetchInfo(from: String): RemoteApkInfo {
        val service = RetrofitClient.SERVICE
        return service.getRssFeed(from).result().channel.item.firstOrNull { item ->
            listOf("alpha", "beta").none { item.title.lowercase().contains(it) }
        }?.link?.let { release ->
            service.getApkMirrorButton(release).result().let {
                RemoteApkInfo(it.link, it.versionName)
            }
        }?.let { intermediate ->
            service.getApkMirrorInputForm(intermediate.link).result().let {
                RemoteApkInfo(it.link, intermediate.version)
            }
        } ?: throw Exception("Failed to fetch apk info from the server: ${server()}")
    }
}
