package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.remote.ApkMirrorService
import app.neonorbit.mrvpatchmanager.util.Utils
import pl.droidsonroids.jspoon.annotation.Selector

@Suppress("MemberVisibilityCanBePrivate")
class ApkMirrorButtonData {
    @Selector(value = ".downloadButton", attr = "href")
    private lateinit var _link: String

    val link: String get() = Utils.absoluteUrl(
        ApkMirrorService.BASE_URL, _link
    )

    @Selector(value = ".appspec-value", defValue = "")
    private var details: String? = null

    val versionName: String? get() = version?.first
    val versionCode: Long? get() = version?.second

    private val version: Pair<String, Long?>? by lazy {
        try {
            details?.substringAfter("Version:", "")?.trim()?.split(' ')?.takeIf {
                it.size >= 2 && it[0].contains('.')
            }?.let {
                val verName = it[0]
                val verCode = try {
                    it[1].trim('(',')').toLong()
                } catch (_: Exception) { null }
                Pair(verName, verCode)
            }
        } catch (_: Exception) { null }
    }

    override fun toString(): String {
        return "versionName: $versionName, versionCode: $versionCode, link: $link"
    }
}
