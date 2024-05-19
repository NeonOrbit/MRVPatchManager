package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.data.AppType
import app.neonorbit.mrvpatchmanager.network.RetrofitClient
import app.neonorbit.mrvpatchmanager.remote.data.ApkMirrorReleaseData
import app.neonorbit.mrvpatchmanager.remote.data.RemoteApkInfo
import app.neonorbit.mrvpatchmanager.result
import app.neonorbit.mrvpatchmanager.util.Utils.LOG

object ApkMirrorService : ApkRemoteService {
    const val BASE_URL = "https://www.apkmirror.com"
    private const val META_URL = "$BASE_URL/apk/facebook-2"
    private const val FALLBACK_URL = "$BASE_URL/uploads/?appcategory="
    private const val VARIANT_FEED_BEG = "variant-{\"arches_slug\":[\""
    private const val VARIANT_FEED_END = "\"]}/feed/"

    private val ids = mapOf(
        AppType.FACEBOOK to "facebook",
        AppType.MESSENGER to "messenger",
        AppType.FACEBOOK_LITE to "lite",
        AppType.MESSENGER_LITE to "messenger-lite",
        AppType.BUSINESS_SUITE to "pages-manager",
    )

    private fun buildFallbackUrl(type: AppType) = "$FALLBACK_URL/${ids[type]}"

    private fun buildFeedUrl(type: AppType, abi: String): String {
        return "$META_URL/${ids[type]}/$VARIANT_FEED_BEG$abi$VARIANT_FEED_END"
    }

    override fun server(): String {
        return "apkmirror.com"
    }

    override suspend fun fetch(type: AppType, abi: String, ver: String?): RemoteApkInfo {
        try {
            return fetchInfo(buildFeedUrl(type, abi), ver) ?:
            fallback(buildFallbackUrl(type), abi, ver) ?: throw Exception()
        } catch (exception: Exception) {
            exception.handleApkServiceException(type, ver)
        }
    }

    private suspend fun fetchInfo(from: String, ver: String?): RemoteApkInfo? {
        return RetrofitClient.SERVICE.getApkMirrorFeed(from).result().channel.items.LOG("Items").filter { item ->
            ApkConfigs.isValidRelease(item.title) && ApkConfigs.matchApkVersion(item.version, ver)
                    && ApkConfigs.isSupportedDPI(item.dpi) && ApkConfigs.isSupportedMinSdk(item.minSDk)
        }.LOG("Filtered").sortedWith(
            ApkConfigs.compareLatest({ it.version }, { ApkConfigs.isPreferredDPI(it.dpi) })
        ).LOG("Sorted").take(3).firstNotNullOfOrNull { item ->
            fetchDownloadLink(item.link, item.version)
        }.LOG("Selected")
    }

    private suspend fun fetchDownloadLink(from: String, version: String?): RemoteApkInfo? {
        return RetrofitClient.SERVICE.getApkMirrorItem(from).result().LOG("ItemData").takeIf { it.isValidType }?.let {
            RemoteApkInfo(it.link, it.version?: version)
        }?.let { info ->
            RetrofitClient.SERVICE.getApkMirrorInputForm(info.link).result().LOG("InputForm").let {
                RemoteApkInfo(it.link, info.version)
            }
        }
    }

    private suspend fun fallback(from: String, abi: String, ver: String?): RemoteApkInfo? {
        return RetrofitClient.SERVICE.getApkMirrorRelease(from).result().releases.LOG("Fallback").filter { release ->
            ApkConfigs.isValidRelease(release.name) && ApkConfigs.matchApkVersion(release.version, ver)
        }.LOG("Filtered").sortedWith(
            ApkConfigs.compareLatest({ it.version })
        ).LOG("Sorted").take(3).selectApk(abi)?.let {
            fetchDownloadLink(it.link, it.version)
        }.LOG("Selected")
    }

    private suspend fun List<ApkMirrorReleaseData.Release>.selectApk(abi: String) = firstNotNullOfOrNull { release ->
        RetrofitClient.SERVICE.getApkMirrorVariant(release.link).result().variants.LOG("Variants").filter {
            it.isValidType && it.arch.lowercase().contains(abi) &&
                    ApkConfigs.isSupportedDPI(it.dpi) && ApkConfigs.isSupportedMinSdk(it.minSDk)
        }.LOG("Filtered").let { filtered ->
            filtered.firstOrNull { ApkConfigs.isPreferredDPI(it.dpi) } ?: filtered.firstOrNull()
        }
    }
}
