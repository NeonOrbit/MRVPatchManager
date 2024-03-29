package app.neonorbit.mrvpatchmanager.apk

import android.os.Build
import app.neonorbit.mrvpatchmanager.compareVersion
import app.neonorbit.mrvpatchmanager.util.Utils

object ApkConfigs {
    const val ARM_64 = "arm64-v8a"
    const val ARM_32 = "armeabi-v7a"
    const val X86_64 = "x86_64"
    const val X86 = "x86"
    val SUPPORTED_ABIs = arrayOf(ARM_64, ARM_32)

    const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    private const val PREFERRED_DPI = "nodpi"
    private val SUPPORTED_DPIs = arrayOf(
        "nodpi", "360dpi", "400dpi", "420dpi", "480dpi", "560dpi", "640dpi"
    )
    private val TEST_BUILDS = arrayOf("alpha", "beta")

    fun isValidRelease(name: String) = name.isNotBlank() && name.lowercase().let { lower ->
        TEST_BUILDS.none { lower.contains(it) }
    }

    fun isValidVersion(name: String, version: String?): Boolean {
        if (version == null) return true
        val ver = version.trim().trimStart('v').split('.')
        val apk = extractVersionName(name)?.split('.') ?: return false
        if (ver.size > apk.size) return false
        for (i in 0 until (ver.size.coerceAtMost(apk.size))) {
            if (ver[i] != apk[i]) return false
        }
        return true
    }

    fun isValidDPI(name: String) = name.lowercase().let { lower ->
        "dpi" !in lower || SUPPORTED_DPIs.any { lower.contains(it) }
    }

    fun isPreferredDPI(name: String) = name.lowercase().contains(PREFERRED_DPI)

    private val VERSION_REGEX: Regex by lazy {
        Regex("\\b(?<!\\.)(\\d+(?:\\.\\d+){3,5})(?!\\.)\\b")
    }

    fun extractVersionName(str: String): String? {
        return VERSION_REGEX.find(str)?.groupValues?.getOrNull(1)
    }

    private val MIN_ANDROID_REGEX: Regex by lazy {
        Regex("\\bAndroid\\s*\\W+(\\d+)(?:\\.\\d+)*\\+")
    }

    private fun extractMinVersion(str: String): Int? {
        return MIN_ANDROID_REGEX.find(str)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    fun isSupportedMinVersion(str: String): Boolean {
        return extractMinVersion(str)?.let { it <= ANDROID_VERSION } != false
    }

    private val ANDROID_VERSION = Utils.sdkToVersion(Build.VERSION.SDK_INT)

    fun <T> compareLatest(selector: (T) -> String): Comparator<T> {
        return Comparator<T> { o1, o2 ->
            extractVersionName(selector(o1))?.compareVersion(extractVersionName(selector(o2))) ?: 0
        }.reversed()
    }

    fun isValidVersionString(str: String): Boolean {
        return str.isNotBlank() && str.split('.').all { it.toIntOrNull() != null }
    }
}
