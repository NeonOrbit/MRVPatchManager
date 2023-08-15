package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.isConnectError
import app.neonorbit.mrvpatchmanager.network.HttpSpec
import app.neonorbit.mrvpatchmanager.network.RetrofitClient
import app.neonorbit.mrvpatchmanager.remote.data.ApkPureItemData
import app.neonorbit.mrvpatchmanager.remote.data.ApkPureReleaseData
import app.neonorbit.mrvpatchmanager.remote.data.RemoteApkInfo
import app.neonorbit.mrvpatchmanager.result
import app.neonorbit.mrvpatchmanager.util.Utils.LOG
import kotlin.coroutines.cancellation.CancellationException

object ApkPureService : ApkRemoteService {
    const val BASE_URL = "https://www.apkpure.com"
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
            RetrofitClient.SERVICE.getApkPureRelease(from).result().releases.LOG("Releases").filter {
                it.isValidType && ApkConfigs.isValidRelease(it.name) && ApkConfigs.isValidVersion(it.name, ver)
            }.take(5).LOG("Filtered").selectApk(abi).LOG("Selected")?.let {
                RemoteApkInfo(it.link, it.version)
            } ?: throw Exception()
        } catch (exception: Exception) {
            exception.handleApkServiceException(type, ver, abi == ApkConfigs.ARM_64)
            null
        }
    }

    private suspend fun  List<ApkPureReleaseData.Release>.selectApk(abi: String) = firstNotNullOfOrNull {
        if (it.isVariant) getItemFromVariants(it.link, abi) else getItemDirectly(it.link, abi)
    }

    private suspend fun  getItemFromVariants(from: String, abi: String): ApkPureItemData.Item? {
        return RetrofitClient.SERVICE.getApkPureVariant(from).result().let { pure ->
            pure.variants.LOG("Variants").filter {
                ApkConfigs.isValidDPI(it.dpi) && ApkConfigs.isSupportedMinVersion(it.min)
            }.let { variants ->
                variants.filter {
                    it.arch.lowercase().contains(abi)
                }.ifEmpty {
                    variants.filter { matchArch(it.arch, abi) }
                }
            }.LOG("Filtered").let { filtered ->
                filtered.firstOrNull {
                    ApkConfigs.isPreferredDPI(it.dpi)
                }?.let { getItemDirectly(it.link, abi) } ?: filtered.firstNotNullOfOrNull {
                    getItemDirectly(it.link, abi)
                }
            }
        }
    }

    private suspend fun getItemDirectly(from: String, abi: String): ApkPureItemData.Item? {
        return RetrofitClient.SERVICE.getApkPureItem(from).result().LOG("ItemData").let { data ->
            data.item?.takeIf { it.isValid(abi) } ?: data.variants.firstOrNull { it.isValid(abi) }
        }
    }

    private fun ApkPureItemData.Item.isValid(abi: String): Boolean {
        return isValidType && matchArch(arch, abi) && ApkConfigs.isSupportedMinVersion(min)
    }

    private fun matchArch(arch: String, abi: String) = arch.lowercase().let { a ->
        a.contains(abi) || (abi == ApkConfigs.ARM_64 && ApkConfigs.SUPPORTED_ABIs.any { a.contains(it) })
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
