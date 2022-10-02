package app.neonorbit.mrvpatchmanager.apk

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import app.neonorbit.mrvpatchmanager.AppConfig
import app.neonorbit.mrvpatchmanager.AppServices
import java.io.File
import java.util.Collections

object ApkUtil {
    const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    private fun PackageInfo.matchSignature(other: String): Boolean {
        return this.signingInfo.apkContentsSigners.any { signature ->
            Signature(other) == signature
        }
    }

    private fun PackageInfo.matchSignature(other: PackageInfo): Boolean {
        val otherSig = other.signingInfo.apkContentsSigners
        return this.signingInfo.apkContentsSigners.any { it in otherSig }
    }

    fun verifyFbSignature(file: File): Boolean {
        return getSignatures(file)?.any { signature ->
            Signature(AppConfig.DEFAULT_FB_SIGNATURE) == signature
        } == true
    }

    fun verifyMrvSignature(file: File): Boolean {
        return getSignatures(file)?.any { signature ->
            Signature(AppConfig.MRV_PUBLIC_SIGNATURE) == signature
        } == true
    }

    fun hasLatestMrvSignedApp(file: File): Boolean {
        return getPackageInfo(file)?.let { apk ->
            getPackageInfo(apk.packageName, true)?.takeIf { installed ->
                installed.matchSignature(AppConfig.MRV_PUBLIC_SIGNATURE)
            }?.let { installed -> installed.longVersionCode >= apk.longVersionCode }
        } == true
    }

    fun getApkSimpleInfo(file: File): ApkSimpleInfo? {
        return getPackageInfo(file, cert = false, meta = true)?.let {
            ApkSimpleInfo(it.packageName, it.getAppName(), it.versionName)
        }
    }

    fun getApkVersionName(file: File) = getPackageInfo(file)?.versionName

    fun getPrefixedVersionName(pkg: String) = getPackageInfo(pkg)?.let { "v${it.versionName}" }

    @Suppress("deprecation")
    fun getConflictedApps(file: File): Map<String, String> {
        val conflicted = HashMap<String, String>()
        getPackageInfo(file, true)?.let { apk ->
            (if (apk.packageName in AppConfig.DEFAULT_FB_PACKAGES) {
                AppConfig.DEFAULT_FB_PACKAGES
            } else {
                Collections.singleton(apk.packageName)
            }).forEach { pkg ->
                getPackageInfo(pkg, cert = true, meta = true)?.let { installed ->
                    if (!installed.matchSignature(apk)) {
                        conflicted[pkg] = installed.getAppName()
                    }
                }
            }
        }
        return conflicted
    }

    private fun PackageInfo.getAppName(): String {
        return AppServices.packageManager.getApplicationLabel(this.applicationInfo).toString()
    }

    private fun getSignatures(file: File) = getPackageInfo(file, true)?.signingInfo?.apkContentsSigners

    @Suppress("deprecation")
    private fun getPackageInfo(pkg: String, cert: Boolean = false): PackageInfo? {
        return try {
            AppServices.packageManager.getPackageInfo(
                pkg, if (cert) PackageManager.GET_SIGNING_CERTIFICATES else 0
            )
        } catch (_: PackageManager.NameNotFoundException) { null }
    }

    @Suppress("deprecation")
    private fun getPackageInfo(file: File, cert: Boolean = false): PackageInfo? {
        return AppServices.packageManager.getPackageArchiveInfo(
            file.absolutePath, if (cert) PackageManager.GET_SIGNING_CERTIFICATES else 0
        )
    }

    @Suppress("Deprecation", "SameParameterValue")
    private fun getPackageInfo(pkg: String, cert: Boolean, meta: Boolean): PackageInfo? {
        return try {
            AppServices.packageManager.getPackageInfo(pkg, getPackageInfoFlags(cert, meta))
        } catch (_: PackageManager.NameNotFoundException) { null }
    }

    @Suppress("Deprecation", "SameParameterValue")
    private fun getPackageInfo(file: File, cert: Boolean, meta: Boolean): PackageInfo? {
        return AppServices.packageManager.getPackageArchiveInfo(
            file.absolutePath, getPackageInfoFlags(cert, meta)
        )
    }

    private fun getPackageInfoFlags(cert: Boolean, meta: Boolean): Int {
        var flag = 0
        if (meta) flag = flag or PackageManager.GET_META_DATA
        if (cert) flag = flag or PackageManager.GET_SIGNING_CERTIFICATES
        return flag
    }

    data class ApkSimpleInfo(val pkg: String, val name: String, val version: String)
}
