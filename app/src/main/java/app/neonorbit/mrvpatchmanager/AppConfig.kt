package app.neonorbit.mrvpatchmanager

import android.os.Build
import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.data.AppItemData
import org.lsposed.lspatch.share.Constants
import org.lsposed.lspatch.share.ConstantsM
import java.io.File

object AppConfig {
    const val APP_TAG = "MRVPatchManager"
    const val FILE_PROVIDER_AUTH = "${BuildConfig.APPLICATION_ID}.file.provider"

    private const val DOWNLOAD_DIR_NAME = "download"
    private const val CACHE_TEMP_DIR_NAME = "temp_dir"
    private const val PATCHED_APK_DIR_NAME = "patched"
    private const val PATCHED_OUT_DIR_NAME = "patch_out"
    private const val CUSTOM_KEYSTORE_NAME = "keystore.bks"

    const val MODULE_PACKAGE = "app.neonorbit.chatheadenabler"
    const val MANAGER_PACKAGE = "app.neonorbit.mrvpatchmanager"

    const val MODULE_APK_NAME = "Module.apk"
    const val MANAGER_APK_NAME = "Manager.apk"
    const val MODULE_ASSET_NAME = "module.pkg"

    val TEMP_DIR: File get() = AppServices.getCacheDir(CACHE_TEMP_DIR_NAME)

    val DOWNLOAD_DIR: File get() = AppServices.getCacheDir(DOWNLOAD_DIR_NAME)

    val PATCHED_OUT_DIR: File get() = AppServices.getCacheDir(PATCHED_OUT_DIR_NAME)

    val PATCHED_APK_DIR: File get() = AppServices.getFilesDir(PATCHED_APK_DIR_NAME)

    val CUSTOM_KEYSTORE_FILE: File get() = File(AppServices.appFilesDir, CUSTOM_KEYSTORE_NAME)

    fun getDownloadApkFile(type: AppType, targetVersion: String?) = File(DOWNLOAD_DIR, "${
        type.getName() + if (targetVersion == null) "" else "-versioned"
    }.apk")

    fun getPatchedApkFile(file: File) = ApkUtil.getApkSimpleInfo(file)?.let { info ->
        (getFbAppName(info.pkg) ?: info.name.replace(' ', '-')) + "-v${info.version}.apk"
    }?.let { name -> File(PATCHED_APK_DIR, name) }

    val DEVICE_ABI: String by lazy {
        if (ApkConfigs.ARM_64 in Build.SUPPORTED_ABIS) ApkConfigs.ARM_64 else ApkConfigs.ARM_32
    }

    const val DEVELOPER = "NeonOrbit"
    const val HELP_FORUM = "XDA Thread"
    const val GITHUB_REPO = "Github Repo"
    const val DEVELOPER_URL = "https://github.com/NeonOrbit"
    const val TUTORIAL_URL = "https://www.youtube.com/watch?v=UxHSTHam42w"
    const val HELP_FORUM_URL = "https://forum.xda-developers.com/t/4331215"
    const val GITHUB_REPO_URL = "https://github.com/NeonOrbit/MRVPatchManager"
    const val MODULE_LATEST_URL = "https://github.com/NeonOrbit/ChatHeadEnabler/releases/latest"
    const val MANAGER_LATEST_URL = "https://github.com/NeonOrbit/MRVPatchManager/releases/latest"

    const val MESSENGER_PRO_PKG = "tn.amin.mpro2"

    @Suppress("UNCHECKED_CAST")
    val DEFAULT_FB_PACKAGES = ConstantsM.DEFAULT_FB_PACKAGES as Set<String>
    const val MESSENGER_PACKAGE = ConstantsM.DEFAULT_TARGET_PACKAGE
    const val DEFAULT_FB_SIGNATURE = ConstantsM.DEFAULT_FB_SIGNATURE
    const val MRV_PUBLIC_SIGNATURE = ConstantsM.DEFAULT_MRV_SIGNATURE
    const val PACKAGE_MASKED_PREFIX = ConstantsM.MASK_PREFIX
    const val PATCHED_APK_CONFIG_PATH = Constants.CONFIG_ASSET_PATH
    const val PATCHED_APK_PROXY_CLASS = Constants.PROXY_APP_COMPONENT_FACTORY

    fun getFbAppName(type: AppType): String {
        return when(type) {
            AppType.FACEBOOK -> "Facebook"
            AppType.MESSENGER -> "Messenger"
            AppType.FACEBOOK_LITE -> "Facebook-Lite"
            AppType.MESSENGER_LITE -> "Messenger-Lite"
            AppType.BUSINESS_SUITE -> "Business-Suite"
        }
    }

    fun getFbAppPkg(type: AppType): String {
        return when(type) {
            AppType.FACEBOOK -> "com.facebook.katana"
            AppType.MESSENGER -> "com.facebook.orca"
            AppType.FACEBOOK_LITE -> "com.facebook.lite"
            AppType.MESSENGER_LITE -> "com.facebook.mlite"
            AppType.BUSINESS_SUITE -> "com.facebook.pages.app"
        }
    }

    fun getFbAppName(pkg: String): String? {
        return when(pkg) {
            "com.facebook.katana" -> "Facebook"
            "com.facebook.orca" -> "Messenger"
            "com.facebook.lite" -> "Facebook-Lite"
            "com.facebook.mlite" -> "Messenger-Lite"
            "com.facebook.pages.app" -> "Business-Suite"
            else -> null
        }
    }

    val FB_APP_LIST by lazy {
        listOf(
            AppItemData("Messenger App", AppType.MESSENGER, R.drawable.ic_fb_orca),
            AppItemData("Facebook App", AppType.FACEBOOK, R.drawable.ic_fb_katana),
            AppItemData("Facebook Lite", AppType.FACEBOOK_LITE, R.drawable.ic_fb_lite),
            AppItemData("Business Suite", AppType.BUSINESS_SUITE, R.drawable.ic_fb_page),
        )
    }

    val FB_ORDERED_PKG_LIST = listOf(
        "com.facebook.katana", "com.facebook.orca", "com.facebook.lite", "com.facebook.mlite", "com.facebook.pages.app"
    )
    val FB_EXCLUDED_PKG_LIST = listOf("com.facebook.services", "com.facebook.system", "com.facebook.appmanager")
}
