package app.neonorbit.mrvpatchmanager.remote.data

import pl.droidsonroids.jspoon.annotation.Selector

class ApkMirrorButtonData {
    @Selector(value = ".downloadButton", attr = "href")
    lateinit var link: String

    @Selector(value = ".appspec-value", defValue = "")
    private var details: String? = null

    val versionName: String? get() = version?.first
    val versionCode: Long? get() = version?.second

    private val version: Pair<String, Long>? by lazy {
        try {
            details?.substringAfter("Version:", "")?.trim()?.split(' ')?.takeIf {
                it.size >= 2 && it[0].contains('.') && it[1].endsWith(')')
            }?.let {
                Pair(it[0], it[1].trim('(',')').toLong())
            }
        } catch (_: Throwable) { null }
    }
}
