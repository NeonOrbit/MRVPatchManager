package app.neonorbit.mrvpatchmanager.apk

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import app.neonorbit.mrvpatchmanager.AppServices

object ApkUtil {
    const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    fun getPrefixedVersionName(pkg: String) = getPackageInfo(pkg)?.let { "v${it.versionName}" }

    @Suppress("Deprecation")
    private fun getPackageInfo(pkg: String, cert: Boolean = false): PackageInfo? {
        return try {
            AppServices.packageManager.getPackageInfo(
                pkg, if (cert) PackageManager.GET_SIGNING_CERTIFICATES else 0
            )
        } catch (_: PackageManager.NameNotFoundException) { null }
    }
}
