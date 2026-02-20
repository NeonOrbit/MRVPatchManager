package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
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

        @Selector("a.version", attr = "href")
        private lateinit var href: String

        @Selector(".vername", defValue = "")
        lateinit var name: String

        val version: String? get() = ApkConfigs.extractVersionName(name)

        val link: String get() = Utils.absoluteUrl(ApkFlashService.BASE_URL, href)

        val isValidType: Boolean get() = true
        /*val isValidType: Boolean get() = truetype.lowercase().let {
            "xapk" !in it || "apk" in it.replace("xapk", "")
        }*/

        override fun toString(): String {
            return "type: $type, name: $name, link: $link"
        }
    }
}
