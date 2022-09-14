package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.network.RetrofitClient
import app.neonorbit.mrvpatchmanager.result

object ApkMirrorService : ApkRemoteService {
    private const val BASE_URL = "https://www.apkmirror.com"
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
        return service.getRssFeed(from).result().channel.item.firstOrNull { item ->
            listOf("alpha", "beta").none { item.title.lowercase().contains(it) }
        }?.link?.let { link ->
            "$BASE_URL${service.getApkMirrorButton(link).result().link}"
        }?.let { link ->
            "$BASE_URL${service.getApkMirrorInputForm(link).result().getLink()}"
        } ?: throw Exception("Failed to fetch direct link")
    }
}
