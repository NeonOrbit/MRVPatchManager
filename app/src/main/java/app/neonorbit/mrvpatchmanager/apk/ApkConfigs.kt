package app.neonorbit.mrvpatchmanager.apk

import android.os.Build
import app.neonorbit.mrvpatchmanager.compareVersion
import app.neonorbit.mrvpatchmanager.util.Utils

object ApkConfigs {
    const val ARM_64 = "arm64-v8a"
    const val ARM_32 = "armeabi-v7a"
    const val X86_64 = "x86_64"
    const val X86 = "x86"

    const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    private const val PREFERRED_DPI = "nodpi"
    private val SUPPORTED_DPIs = arrayOf(
        "nodpi", "360dpi", "400dpi", "420dpi", "480dpi", "560dpi", "640dpi"
    )
    private val TEST_BUILDS = arrayOf("alpha", "beta", ".0.0.0.")

    fun isValidRelease(name: String) = name.isNotBlank() && name.lowercase().let { lower ->
        TEST_BUILDS.none { lower.contains(it) }
    }

    fun matchApkVersion(apkVersion: String?, version: String?): Boolean {
        if (version == null) return true
        if (apkVersion == null) return false
        val ver = version.trim().trimStart('v').split('.')
        val apk = apkVersion.trim().trimStart('v').split('.')
        if (ver.size > apk.size) return false
        for (i in ver.indices) {
            if (ver[i] != apk[i]) return false
        }
        return true
    }

    fun isSupportedDPI(dpi: String?) = dpi?.lowercase()?.let {
        SUPPORTED_DPIs.any { dpi.contains(it) }
    } != false

    fun isPreferredDPI(dpi: String?) = dpi?.lowercase()?.contains(PREFERRED_DPI) != false

    fun isSupportedMinSdk(sdk: Int?) = sdk?.let { it <= ANDROID_VERSION } != false

    private val VERSION_REGEX: Regex by lazy {
        Regex("\\b(?<!\\.)(\\d+(?:\\.\\d+){3,5})(?!\\.)\\b")
    }

    fun extractVersionName(str: String?) = str?.takeIf { it.isNotBlank() }?.let {
        VERSION_REGEX.find(str)?.groupValues?.getOrNull(1)
    }

    private val MIN_SDK_REGEX: Regex by lazy {
        Regex("\\bAndroid\\s*\\W+(\\d+)(?:\\.\\d+)*\\+")
    }

    fun extractMinSdk(str: String): Int? {
        return MIN_SDK_REGEX.find(str)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private val ANDROID_VERSION = Utils.sdkToVersion(Build.VERSION.SDK_INT)

    fun <T> compareLatest(selector: (T) -> String?, then: ((T) -> Boolean)? = null): Comparator<T> {
        return Comparator<T> { o1, o2 ->
            selector(o1)?.compareVersion(selector(o2)) ?: 0
        }.let {
            if (then == null) it else it.thenComparing { o1, o2 ->
                then(o1).compareTo(then(o2))
            }
        }.reversed()
    }

    fun isValidVersionString(str: String): Boolean {
        return str.isNotBlank() && str.split('.').all { it.toIntOrNull() != null }
    }
}
