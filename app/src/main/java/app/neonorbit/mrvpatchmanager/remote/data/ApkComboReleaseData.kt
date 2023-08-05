package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.remote.ApkComboService
import app.neonorbit.mrvpatchmanager.util.Utils
import pl.droidsonroids.jspoon.annotation.Selector

class ApkComboReleaseData {
    @Selector(".list-versions li")
    var releases: List<Release> = listOf()

    override fun toString(): String {
        return "releases: $releases"
    }

    class Release {
        @Selector(".vtype", defValue = "")
        lateinit var type: String

        @Selector(".vername", defValue = "")
        lateinit var name: String

        @Selector("a.ver-item", attr = "href")
        private lateinit var _link: String

        val link: String get() = Utils.absoluteUrl(
            ApkComboService.BASE_URL, _link
        )

        val isValidType: Boolean get() = type.lowercase().let {
            "xapk" !in it || "apk" in it.replace("xapk", "")
        }

        override fun toString(): String {
            return "type: $type, name: $name, link: $link"
        }
    }
}
