package app.neonorbit.mrvpatchmanager.apk

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_META_DATA
import android.content.pm.PackageManager.GET_PERMISSIONS
import android.content.pm.PackageManager.GET_SIGNATURES
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.content.pm.Signature
import android.graphics.drawable.Drawable
import app.neonorbit.mrvpatchmanager.AppConfig
import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.compareVersion
import app.neonorbit.mrvpatchmanager.util.Utils
import java.io.File
import java.util.StringJoiner

object ApkUtil {
    private fun PackageInfo.matchSignature(other: String): Boolean {
        return this.getSignatures().any { signature ->
            Signature(other) == signature
        }
    }

    private fun PackageInfo.matchSignature(other: PackageInfo): Boolean {
        return this.getSignatures().any { it in other.getSignatures() }
    }

    fun isMessenger(app: AppType?): Boolean {
        return app?.getPackage() == AppConfig.MESSENGER_PACKAGE
    }

    fun isMessenger(file: File?): Boolean {
        return file?.let {
            getPackageInfo(it)
        }?.packageName == AppConfig.MESSENGER_PACKAGE
    }

    fun verifySignature(file: File, sig: String): Boolean {
        return getSignatures(file).any { signature ->
            Signature(sig) == signature
        }
    }

    fun verifyFbSignature(file: File, strict: Boolean = true): Boolean {
        return getPackageInfo(file, true)?.takeIf {
            AppConfig.DEFAULT_FB_PACKAGES.contains(it.packageName)
        }?.matchSignature(AppConfig.DEFAULT_FB_SIGNATURE).let {
            if (strict) it == true else it != false
        }
    }

    fun hasLatestMrvSignedApp(file: File, sig: String? = null): Boolean {
        return getPackageInfo(file)?.let { apk ->
            hasLatestMrvSignedApp(apk.packageName, apk.versionName, sig)
        } == true
    }

    fun hasLatestMrvSignedApp(pkg: String, version: String, sig: String? = null): Boolean {
        val signature = sig ?: AppConfig.MRV_PUBLIC_SIGNATURE
        return getPackageInfo(pkg, true)?.takeIf { installed ->
            try {
                installed.matchSignature(signature)
            } catch (_: Exception) { false }
        }?.let { installed ->
            installed.versionName.compareVersion(version) >= 0
        } == true
    }

    fun getApkSimpleInfo(file: File): ApkSimpleInfo? {
        return getPackageInfo(file, cert = false, meta = true)?.let {
            ApkSimpleInfo(it.packageName, it.getAppName(), it.versionName)
        }
    }

    fun getApkIcon(file: File): Drawable? {
        return getPackageInfo(file)?.let {
            it.applicationInfo.sourceDir = file.absolutePath
            it.applicationInfo.publicSourceDir = file.absolutePath
            it.applicationInfo.loadIcon(AppServices.packageManager)
        }
    }

    fun getPrefixedVersionName(pkg: String) = getPackageInfo(pkg)?.let { "v${it.versionName}" }

    fun getConflictedApps(file: File, exact: Boolean): Map<String, String> {
        val conflicted = HashMap<String, String>()
        getPackageInfo(file, true)?.let { apk ->
            getRelatedPackages(apk.packageName, exact).forEach { pkg ->
                getPackageInfo(pkg, cert = true, meta = true)?.let { installed ->
                    try {
                        if (!installed.matchSignature(apk)) {
                            conflicted[pkg] = installed.getAppName()
                        }
                    } catch (_: Exception) { }
                }
            }
        }
        return conflicted
    }

    fun getApkSummery(file: File) = getPackageInfo(file)?.let { info ->
        val summery = StringJoiner(" ")
        summery.add(info.longVersionCode.toString())
        ApkParser.getABI(file)?.let { summery.add("($it)") }
        if (info.isMasked()) summery.add("[masked]")
        summery.toString()
    }

    @Suppress("Deprecation")
    fun getApkDetails(files: List<File>): String {
        val flags = GET_META_DATA or GET_SIGNING_CERTIFICATES or GET_SIGNATURES or GET_PERMISSIONS
        val result = StringJoiner("\n\n")
        files.forEach { file ->
            val details = StringJoiner("\n")
            getPackageInfo(file, flags)?.also { info ->
                details.add("Name: ${info.getAppName()}")
                details.add("Patch: ${if (info.isPatched()) "Patched" else "Signed Only"}")
                details.add("Signature: ${
                    if (info.matchSignature(AppConfig.MRV_PUBLIC_SIGNATURE)) "Default" else "Custom"
                }")
                details.add("Version: ${info.versionName}")
                details.add("Version Code: ${info.longVersionCode}")
                details.add("Min Support: ${info.minAndroidName}")
                details.add("Max Support: ${info.maxAndroidName}")
                details.add("Architecture: ${ApkParser.getABI(file) ?: "unknown"}")
                ApkParser.getPatchedConfig(file)?.let { config ->
                    details.add("Fallback Mode: ${config.fallback}")
                }
            } ?: details.add("Failed to retrieve apk info: ${file.name}")
            result.add(details.toString())
        }
        return result.toString()
    }

    private fun PackageInfo.isMasked() = packageName.startsWith(AppConfig.PACKAGE_MASKED_PREFIX)
    private fun PackageInfo.isPatched() = applicationInfo?.appComponentFactory == AppConfig.PATCHED_APK_PROXY_CLASS
    private val PackageInfo.minAndroidName get() = "Android " + Utils.sdkToVersion(applicationInfo?.minSdkVersion ?: 0)
    private val PackageInfo.maxAndroidName get() = "Android " + Utils.sdkToVersion(applicationInfo?.targetSdkVersion ?: 0)

    private fun getRelatedPackages(pkg: String, exact: Boolean): Set<String> {
        return if (!exact && pkg in AppConfig.DEFAULT_FB_PACKAGES) AppConfig.DEFAULT_FB_PACKAGES else setOf(pkg)
    }

    private fun getSignatures(file: File): Array<out Signature> {
        return getPackageInfo(file, true)?.getSignatures() ?: throw Exception("Failed to read apk signature")
    }

    private fun getPackageInfo(pkg: String, cert: Boolean = false) = getPackageInfo(pkg, getFlags(cert, false))

    @Suppress("SameParameterValue")
    private fun getPackageInfo(file: File, cert: Boolean = false) = getPackageInfo(file, getFlags(cert, false))

    @Suppress("SameParameterValue")
    private fun getPackageInfo(pkg: String, cert: Boolean, meta: Boolean) = getPackageInfo(pkg, getFlags(cert, meta))

    @Suppress("SameParameterValue")
    private fun getPackageInfo(file: File, cert: Boolean, meta: Boolean) = getPackageInfo(file, getFlags(cert, meta))

    @Suppress("Deprecation", "SameParameterValue")
    private fun getPackageInfo(pkg: String, flags: Int): PackageInfo? = try {
        AppServices.packageManager.getPackageInfo(pkg, flags)
    } catch (_: PackageManager.NameNotFoundException) { null }

    @Suppress("Deprecation", "SameParameterValue")
    private fun getPackageInfo(file: File, flags: Int): PackageInfo? {
        return AppServices.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
    }

    @Suppress("Deprecation")
    private fun getFlags(cert: Boolean, meta: Boolean): Int {
        var flag = 0
        if (meta) flag = flag or GET_META_DATA
        if (cert) flag = flag or GET_SIGNING_CERTIFICATES or GET_SIGNATURES
        return flag
    }

    private fun PackageInfo.getAppName(): String {
        return AppConfig.getFbAppName(packageName) ?:
        AppServices.packageManager.getApplicationLabel(this.applicationInfo).let {
            if (it.startsWith(this.packageName)) this.packageName else it.toString()
        }
    }

    @Suppress("Deprecation")
    private fun PackageInfo.getSignatures() = signingInfo?.apkContentsSigners ?: signatures

    data class ApkSimpleInfo(val pkg: String, val name: String, val version: String)
}
