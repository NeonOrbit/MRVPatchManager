package app.neonorbit.mrvpatchmanager.apk

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_META_DATA
import android.content.pm.PackageManager.GET_PERMISSIONS
import android.content.pm.PackageManager.GET_SIGNATURES
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.content.pm.Signature
import android.graphics.drawable.Drawable
import app.neonorbit.mrvpatchmanager.AppConfigs
import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.compareVersion
import app.neonorbit.mrvpatchmanager.data.AppFileData
import app.neonorbit.mrvpatchmanager.data.AppType
import app.neonorbit.mrvpatchmanager.toSizeString
import app.neonorbit.mrvpatchmanager.util.Utils
import java.io.File
import java.util.StringJoiner
import java.util.zip.ZipFile

object ApkUtil {
    data class ApkSimpleInfo(val pkg: String, val name: String, val version: String)

    fun isApk(file: File, verify: Boolean = false): Boolean {
        return file.extension.equals("apk", true) &&
                (!verify || ZipFile(file).use { it.getEntry(ApkConfigs.ANDROID_MANIFEST) != null })
    }

    private fun PackageInfo.matchSignature(other: String): Boolean {
        return this.getSignatures().matchSignature(Signature(other))
    }

    private fun PackageInfo.matchSignature(other: PackageInfo): Boolean {
        return this.getSignatures().matchSignature(other.getSignatures())
    }

    private fun Array<Signature>.matchSignature(other: Signature) = any { it == other }
    private fun Array<Signature>.matchSignature(others: Array<Signature>) = any { it in others }

    fun verifySignature(file: File, sig: String): Boolean {
        return ApkBundles.peekBaseApkFromBundle(file) { base ->
            getSignatures(base ?: file).matchSignature(Signature(sig))
        }
    }

    fun verifyFbSignature(file: File, strict: Boolean = true): Boolean {
        return getPackageInfo(file, true)?.takeIf {
            AppConfigs.DEFAULT_FB_PACKAGES.contains(it.packageName)
        }?.matchSignature(AppConfigs.DEFAULT_FB_SIGNATURE).let {
            if (strict) it == true else it != false
        }
    }

    fun verifyFbSignatureWithVersion(file: File, version: String?): Boolean {
        return getPackageInfo(file, true)?.takeIf {
            ApkConfigs.matchApkVersion(it.versionName, version) &&
            AppConfigs.DEFAULT_FB_PACKAGES.contains(it.packageName)
        }?.matchSignature(AppConfigs.DEFAULT_FB_SIGNATURE) == true
    }

    fun hasLatestMrvSignedApp(file: File, sig: String? = null): Boolean {
        return getPackageInfo(file)?.let { apk ->
            hasLatestMrvSignedApp(apk.packageName, apk.versionName!!, sig)
        } == true
    }

    fun hasLatestMrvSignedApp(pkg: String, version: String, sig: String? = null): Boolean {
        val signature = sig ?: AppConfigs.MRV_PUBLIC_SIGNATURE
        return getPackageInfo(pkg, true)?.takeIf { installed ->
            try {
                installed.matchSignature(signature)
            } catch (_: Exception) { false }
        }?.let { installed ->
            installed.versionName!!.compareVersion(version) >= 0
        } == true
    }

    fun getApkSimpleInfo(file: File): ApkSimpleInfo? {
        return getPackageInfo(file, cert = false, meta = true)?.let {
            ApkSimpleInfo(it.packageName, it.getAppName(), it.versionName!!)
        }
    }

    fun getApkIcon(file: File): Drawable? {
        return if (isApk(file)) getPackageInfo(file)?.let {
            it.applicationInfo!!.sourceDir = file.absolutePath
            it.applicationInfo!!.publicSourceDir = file.absolutePath
            it.applicationInfo!!.loadIcon(AppServices.packageManager)
        } else ApkBundles.getIconFromBundle(file)
    }

    fun getPrefixedVersionName(pkg: String) = getPackageInfo(pkg)?.let { "v${it.versionName}" }

    fun getConflictedApps(file: File): Map<String, String> {
        val conflicted = HashMap<String, String>()
        val base = ApkBundles.getBaseApkFromBundle(file)
        try {
            getPackageInfo(base ?: file, cert = true, meta = false, perm = true)?.let { apk ->
                getRelatedPackages(apk.packageName).forEach { pkg ->
                    getPackageInfo(pkg, cert = true, meta = true, perm = true)?.let { installed ->
                        try {
                            if (!installed.matchSignature(apk) &&
                                (apk.packageName == installed.packageName ||
                                        match(apk.isPermissionMasked, installed.isPermissionMasked))) {
                                conflicted[pkg] = installed.getAppName()
                            }
                        } catch (_: Exception) { }
                    }
                }
            }
        } finally {
            base?.delete()
        }
        return conflicted
    }

    private fun match(a: Boolean?, b: Boolean?): Boolean = a != null && b != null && a == b

    fun getApkSummery(file: File): String? {
        return if (isApk(file)) getPackageInfo(file)?.let { info ->
            val summery = StringJoiner(" ")
            summery.add(info.longVersionCode.toString())
            ApkParser.getABI(file)?.let { summery.add("($it)") }
            if (info.isMasked()) summery.add("[masked]")
            summery.toString()
        } else ApkBundles.getSummeryFromBundle(file)
    }

    fun getApkDetails(files: List<File>): String {
        val result = StringJoiner("\n\n")
        files.forEach { raw ->
            val details = StringJoiner("\n")
            val size = raw.length().toSizeString(true)
            val file = ApkBundles.getBaseApkFromBundle(raw) ?: raw
            val bundleText = if (file !== raw) "(Bundle)" else ""
            try {
                val info = getPackageInfo(file, cert = true, meta = true, perm = true) ?: throw Exception()
                details.add("[${info.packageName}]")
                details.add("App: ${info.getAppName()}")
                details.add("Size: $size $bundleText")
                details.add("Status: ${if (info.isPatched()) "Patched" else "Signed Only"}")
                details.add("Signature: ${
                    if (info.matchSignature(AppConfigs.MRV_PUBLIC_SIGNATURE)) "Default" else "Custom"
                }")
                details.add("Version: ${info.versionName}")
                details.add("Version Code: ${info.longVersionCode}")
                details.add("Min SDK: ${info.minAndroidName}")
                details.add("Target SDK: ${info.maxAndroidName}")
                details.add("Architecture: ${ApkParser.getABI(file) ?: "unknown"}")
                ApkParser.getPatchedConfig(file)?.also { config ->
                    details.add("Fallback Mode: ${config.fallback}")
                    details.add("Package Masked: ${config.pkgMasked}")
                    details.add("Resolved Conflicts: ${config.confFixed}")
                    if (config.exModules.isNotEmpty()) {
                        details.add("Modules: ${config.exModules}")
                    }
                } ?: info.isPermissionMasked?.also {
                    if (it) details.add("Resolved Conflicts: true")
                }
            } catch (e: Exception) {
                details.add("File: ${raw.name}").add("Size: $size")
                details.add("Failed to retrieve apk info: ${e.message ?: ""}")
            } finally {
                if (file !== raw) file.delete()
            }
            result.add(details.toString())
        }
        return result.toString()
    }

    @Suppress("QueryPermissionsNeeded")
    fun getInstalledAppList() = AppServices.packageManager.getInstalledApplications(0).filter {
        it.packageName.let { pkg ->
            (pkg.startsWith("com.facebook.") && pkg !in AppConfigs.FB_EXCLUDED_PKG_LIST) || pkg.startsWith("com.instagram.")
        }
    }.mapNotNull { getPackageInfo(it.packageName) }.sortedWith(Comparator.comparing {
       AppConfigs.FB_ORDERED_PKG_LIST.indexOf(it.packageName).takeIf { i -> i >= 0 } ?: AppConfigs.FB_ORDERED_PKG_LIST.size
    }).map { AppFileData(it.getAppName(), File(it.apkPath)) }

    fun isPatched(file: File?) = file?.let { getPackageInfo(it) }?.isPatched() == true

    fun isMessenger(app: AppType?) = app?.getPackage() == AppConfigs.MESSENGER_PACKAGE

    fun isMessenger(file: File?) = file?.let { getPackageInfo(it) }?.packageName == AppConfigs.MESSENGER_PACKAGE

    fun isRecommendedForResolveConflicts(file: File) = getPackageInfo(file)?.packageName?.let {
        it == AppType.FACEBOOK.getPackage() || it == AppType.MESSENGER.getPackage()
    } != false

    private fun PackageInfo.isMasked() = packageName.startsWith(AppConfigs.PACKAGE_MASKED_PREFIX)

    private fun PackageInfo.isPatched() = applicationInfo?.appComponentFactory == AppConfigs.PATCHED_APK_PROXY_CLASS

    private val PackageInfo.minAndroidName get() = "Android " + Utils.sdkToVersion(applicationInfo?.minSdkVersion ?: 0)

    private val PackageInfo.maxAndroidName get() = "Android " + Utils.sdkToVersion(applicationInfo?.targetSdkVersion ?: 0)

    private val PackageInfo.apkPath get() = applicationInfo!!.publicSourceDir?.takeIf { it.isNotEmpty() } ?: applicationInfo!!.sourceDir

    private val PackageInfo.isPermissionMasked get() = permissions?.let { permissions ->
        permissions.any { permission -> permission.name?.startsWith(AppConfigs.PACKAGE_MASKED_PREFIX) == true }
    }

    private fun getRelatedPackages(pkg: String): Set<String> {
        return if (pkg in AppConfigs.DEFAULT_FB_PACKAGES) AppConfigs.DEFAULT_FB_PACKAGES else setOf(pkg)
    }

    @Suppress("Deprecation")
    private fun PackageInfo.getSignatures(): Array<Signature> {
        return ((signatures ?: emptyArray()) + (signingInfo?.apkContentsSigners ?: emptyArray())).also {
            if (it.isEmpty()) throw Exception("Failed to read apk signature")
        }
    }

    private fun getSignatures(file: File): Array<Signature> {
        return getPackageInfo(file, true)?.getSignatures() ?: throw Exception("Failed to read apk signature")
    }

    private fun getPackageInfo(pkg: String, cert: Boolean = false) = getPackageInfo(pkg, getFlags(cert, false))

    @Suppress("SameParameterValue")
    private fun getPackageInfo(file: File, cert: Boolean = false) = getPackageInfo(file, getFlags(cert, false))

    @Suppress("SameParameterValue")
    private fun getPackageInfo(pkg: String, cert: Boolean, meta: Boolean, perm: Boolean = false) = getPackageInfo(
        pkg, getFlags(cert, meta, perm)
    )

    @Suppress("SameParameterValue")
    private fun getPackageInfo(file: File, cert: Boolean, meta: Boolean, perm: Boolean = false) = getPackageInfo(
        file, getFlags(cert, meta, perm)
    )

    @Suppress("SameParameterValue")
    private fun getPackageInfo(pkg: String, flags: Int): PackageInfo? = try {
        AppServices.packageManager.getPackageInfo(pkg, flags)
    } catch (_: PackageManager.NameNotFoundException) { null }

    @Suppress("SameParameterValue")
    private fun getPackageInfo(file: File, flags: Int): PackageInfo? {
        return AppServices.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
    }

    @Suppress("Deprecation")
    private fun getFlags(cert: Boolean, meta: Boolean, perm: Boolean = false): Int {
        var flag = 0
        if (meta) flag = flag or GET_META_DATA
        if (perm) flag = flag or GET_PERMISSIONS
        if (cert) flag = flag or GET_SIGNING_CERTIFICATES or GET_SIGNATURES
        return flag
    }

    private fun PackageInfo.getAppName(): String {
        return AppConfigs.getFbAppName(packageName) ?:
        AppServices.packageManager.getApplicationLabel(this.applicationInfo!!).let {
            if (it.startsWith(this.packageName)) this.packageName else it.toString()
        }
    }
}
