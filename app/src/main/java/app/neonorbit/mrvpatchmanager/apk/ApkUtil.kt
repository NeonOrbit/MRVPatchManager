package app.neonorbit.mrvpatchmanager.apk

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_META_DATA
import android.content.pm.PackageManager.GET_SIGNATURES
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.content.pm.Signature
import android.graphics.drawable.Drawable
import app.neonorbit.mrvpatchmanager.AppConfig
import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.compareVersion
import java.io.File
import java.util.Collections

object ApkUtil {
    const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    private fun PackageInfo.matchSignature(other: String): Boolean {
        return this.getSignatures().any { signature ->
            Signature(other) == signature
        }
    }

    private fun PackageInfo.matchSignature(other: PackageInfo): Boolean {
        return this.getSignatures().any { it in other.getSignatures() }
    }

    fun verifyFbSignature(file: File): Boolean {
        return getSignatures(file).any { signature ->
            Signature(AppConfig.DEFAULT_FB_SIGNATURE) == signature
        }
    }

    fun verifyMrvSignature(file: File): Boolean {
        return getSignatures(file).any { signature ->
            Signature(AppConfig.MRV_PUBLIC_SIGNATURE) == signature
        }
    }

    fun hasLatestMrvSignedApp(file: File): Boolean {
        return getPackageInfo(file)?.let { apk ->
            hasLatestMrvSignedApp(apk.packageName, apk.versionName)
        } == true
    }

    fun hasLatestMrvSignedApp(pkg: String, version: String): Boolean {
        return getPackageInfo(pkg, true)?.takeIf { installed ->
            try {
                installed.matchSignature(AppConfig.MRV_PUBLIC_SIGNATURE)
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

    fun getApkVersionName(file: File) = getPackageInfo(file)?.versionName

    fun getPrefixedVersionName(pkg: String) = getPackageInfo(pkg)?.let { "v${it.versionName}" }

    fun getConflictedApps(file: File): Map<String, String> {
        val conflicted = HashMap<String, String>()
        getPackageInfo(file, true)?.let { apk ->
            (if (apk.packageName in AppConfig.DEFAULT_FB_PACKAGES) {
                AppConfig.DEFAULT_FB_PACKAGES
            } else {
                Collections.singleton(apk.packageName)
            }).forEach { pkg ->
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

    private fun getSignatures(file: File): Array<out Signature> {
        return getPackageInfo(file, true)?.getSignatures() ?: throw Exception(
            "Failed to read apk signature"
        )
    }

    @Suppress("deprecation")
    private fun getPackageInfo(pkg: String, cert: Boolean = false): PackageInfo? {
        return try {
            AppServices.packageManager.getPackageInfo(
                pkg, if (cert) (GET_SIGNING_CERTIFICATES or GET_SIGNATURES) else 0
            )
        } catch (_: PackageManager.NameNotFoundException) { null }
    }

    @Suppress("deprecation")
    private fun getPackageInfo(file: File, cert: Boolean = false): PackageInfo? {
        return AppServices.packageManager.getPackageArchiveInfo(
            file.absolutePath, if (cert) (GET_SIGNING_CERTIFICATES or GET_SIGNATURES) else 0
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

    @Suppress("deprecation")
    private fun getPackageInfoFlags(cert: Boolean, meta: Boolean): Int {
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

    @Suppress("deprecation")
    private fun PackageInfo.getSignatures() = signingInfo?.apkContentsSigners ?: signatures

    data class ApkSimpleInfo(val pkg: String, val name: String, val version: String)
}
