package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.remote.ApkMirrorService
import app.neonorbit.mrvpatchmanager.util.Utils
import pl.droidsonroids.jspoon.annotation.Selector

class ApkMirrorItemData {
    @Selector(value = ".downloadButton")
    private var links: List<Link> = listOf()

    @Selector(value = ".app-title", defValue = "")
    private lateinit var title: String

    @Selector(value = ".appspec-value:matches(\\b(?<!\\.)(\\d+(?:\\.\\d+){3,5})(?!\\.)\\b)", defValue = "")
    private lateinit var info: String

    val isValidType: Boolean get() = links.any { it.isValid }

    val version: String? get() = ApkConfigs.extractVersionName(info) ?: ApkConfigs.extractVersionName(title)

    val link: String get() = Utils.absoluteUrl(ApkMirrorService.BASE_URL, links.first { it.isValid }.href)

    override fun toString(): String {
        return "type: ${if (isValidType) "apk" else "xapk"}, version: $version, link: $links"
    }

    private class Link {
        @Selector(value = "a", attr = "href", defValue = "")
        lateinit var href: String

        @Selector(value = "a", defValue = "")
        lateinit var text: String

        val isValid: Boolean get() = href.contains("forcebaseapk=true") || text.lowercase().let {
            "apk" in it && "bundle" !in it
        }

        override fun toString(): String {
            return "text: $text, url: $href"
        }
    }
}
