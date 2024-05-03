package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.remote.ApkFlashService
import app.neonorbit.mrvpatchmanager.util.Utils
import pl.droidsonroids.jspoon.annotation.Selector

class ApkFlashReleaseData {
    @Selector(value = "ul.list-versions > li > a.version")
    var releases: List<Release> = listOf()

    override fun toString(): String {
        return "releases: $releases"
    }

    class Release {
        @Selector(".vtype", defValue = "")
        private lateinit var type: String

        @Selector(".vername", defValue = "")
        lateinit var name: String

        @Selector("a.version", attr = "href")
        private lateinit var _link: String

        val link: String get() = Utils.absoluteUrl(
            ApkFlashService.BASE_URL, _link
        )

        val isValidType: Boolean get() = type.lowercase().let {
            "xapk" !in it || "apk" in it.replace("xapk", "")
        }

        override fun toString(): String {
            return "type: $type, name: $name, link: $link"
        }
    }
}
