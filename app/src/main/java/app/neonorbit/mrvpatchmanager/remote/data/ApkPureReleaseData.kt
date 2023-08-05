package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.remote.ApkPureService
import app.neonorbit.mrvpatchmanager.util.Utils
import pl.droidsonroids.jspoon.annotation.Selector

class ApkPureReleaseData {
    @Selector(value = "a.ver_download_link")
    var releases: List<Release> = listOf()

    override fun toString(): String {
        return "releases: $releases"
    }

    class Release {
        @Selector(".ver-item-type", defValue = "")
        private lateinit var type: String

        @Selector(".ver-item-n", defValue = "")
        lateinit var name: String

        @Selector("a.ver_download_link", attr = "href")
        private lateinit var _link: String

        val link: String get() = Utils.absoluteUrl(
            ApkPureService.BASE_URL, _link
        )

        val isVariant: Boolean get() = _link.contains("/variant/")

        val isValidType: Boolean get() = type.trim().lowercase().let { it == "apk" || "xapk" !in it }

        override fun toString(): String {
            return "type: $type, name: $name, link: $link"
        }
    }
}
