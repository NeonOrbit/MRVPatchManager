package app.neonorbit.mrvpatchmanager.apk

import android.os.Build
import app.neonorbit.mrvpatchmanager.compareVersion

object ApkConfigs {
    const val ARM_64 = "arm64-v8a"
    const val ARM_32 = "armeabi-v7a"
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

    private fun extractMinSdk(str: String): Int? {
        return MIN_ANDROID_REGEX.find(str)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    fun isSupportedMinVersion(str: String): Boolean {
        return extractMinSdk(str)?.let { it <= ANDROID_VERSION } != false
    }

    private val ANDROID_VERSION = when (Build.VERSION.SDK_INT) {
        28 -> 9; 29 -> 10; 30 -> 11; 31, 32 -> 12
        else -> Build.VERSION.SDK_INT - 20
    }

    fun <T> compareLatest(selector: (T) -> String): Comparator<T> {
        return Comparator<T> { o1, o2 ->
            extractVersionName(selector(o1))?.compareVersion(extractVersionName(selector(o2))) ?: 0
        }.reversed()
    }
}
