package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.network.RetrofitClient
import app.neonorbit.mrvpatchmanager.remote.data.RemoteApkInfo
import app.neonorbit.mrvpatchmanager.remote.data.RssFeedData
import app.neonorbit.mrvpatchmanager.result
import app.neonorbit.mrvpatchmanager.util.Utils.LOG

object ApkMirrorService : ApkRemoteService {
    const val BASE_URL = "https://www.apkmirror.com"
    private const val META_URL = "$BASE_URL/apk/facebook-2"
    private const val VARIANT_FEED_BEG = "variant-{\"arches_slug\":[\""
    private const val VARIANT_FEED_END = "\"]}/feed"

    private val ids = mapOf(
        AppType.FACEBOOK to "facebook",
        AppType.MESSENGER to "messenger",
        AppType.FACEBOOK_LITE to "lite",
        AppType.MESSENGER_LITE to "messenger-lite",
        AppType.BUSINESS_SUITE to "pages-manager",
    )

    private fun buildFeedUrl(type: AppType, abi: String): String {
        return "$META_URL/${ids[type]}/$VARIANT_FEED_BEG$abi$VARIANT_FEED_END"
    }

    private fun removeFeedUrl(url: String) = url.substringBefore("/variant-")

    override fun server(): String {
        return "apkmirror.com"
    }

    override suspend fun fetch(type: AppType, abi: String, ver: String?): RemoteApkInfo {
        try {
            return fetchInfo(buildFeedUrl(type, abi), abi, ver)
        } catch (exception: Exception) {
            exception.handleApkServiceException(type, ver)
        }
    }

    private suspend fun fetchInfo(from: String, abi: String, ver: String?): RemoteApkInfo {
        val service = RetrofitClient.SERVICE
        return service.getRssFeed(from).result().channel.items.LOG("Items").filter { item ->
            ApkConfigs.isValidRelease(item.title) && ApkConfigs.isValidVersion(item.title, ver)
        }.LOG("Filtered").selectDPI().let {
            it ?: fallback(removeFeedUrl(from), abi, ver)
        }.LOG("Selected")?.let { release ->
            service.getApkMirrorItem(release).result().let {
                RemoteApkInfo(it.link, it.versionName)
            }
        }?.let { intermediate ->
            service.getApkMirrorInputForm(intermediate.link).result().let {
                RemoteApkInfo(it.link, intermediate.version)
            }
        } ?: throw Exception()
    }

    private fun List<RssFeedData.RssChannel.RssItem>.selectDPI(): String? {
        if (isEmpty()) return null
        val versionedName = versionedRegex.find(this[0].title)?.groupValues?.getOrNull(1)
        if (versionedName?.isEmpty() != false) return this[0].link
        return this.sortedWith(ApkConfigs.compareLatest { it.title }).filter { item ->
            ApkConfigs.isValidDPI(item.title)
        }.let { filtered ->
            filtered.firstOrNull {
                it.title.startsWith(versionedName) && ApkConfigs.isPreferredDPI(it.title)
            } ?: filtered.firstOrNull()
        }?.link
    }

    private suspend fun fallback(from: String, abi: String, ver: String?): String? {
        return RetrofitClient.SERVICE.getApkMirrorRelease(from).result().releases.LOG("Fallback Releases").filter { release ->
            ApkConfigs.isValidRelease(release.name) && ApkConfigs.isValidVersion(release.name, ver)
        }.sortedWith(ApkConfigs.compareLatest { it.name }).take(3).firstNotNullOfOrNull { release ->
            RetrofitClient.SERVICE.getApkMirrorVariant(release.link).result().variants.LOG("Fallback Variants").filter {
                it.arch.lowercase().contains(abi) && ApkConfigs.isValidDPI(it.dpi) && ApkConfigs.isSupportedMinVersion(it.min)
            }.LOG("Fallback Variants Filtered").let { filtered ->
                filtered.firstOrNull { ApkConfigs.isPreferredDPI(it.dpi) } ?: filtered.firstOrNull()
            }
        }?.link
    }

    private val versionedRegex by lazy { Regex("(.*?\\b(?<!\\.)\\d+(?:\\.\\d+){3,5})(?!\\.)\\b.*") }
}
