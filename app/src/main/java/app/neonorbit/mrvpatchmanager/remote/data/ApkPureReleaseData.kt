package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
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

        @Selector("a.ver_download_link", attr = "href")
        private lateinit var href: String

        @Selector(".ver-item-n", defValue = "")
        lateinit var name: String

        val version: String? get() = ApkConfigs.extractVersionName(name)

        val link: String get() = Utils.absoluteUrl(ApkPureService.BASE_URL, href)

        val isValidType: Boolean get() = true
        /*val isValidType: Boolean get() = type.lowercase().let {
            "xapk" !in it || "apk" in it.replace("xapk", "")
        }*/

        override fun toString(): String {
            return "type: $type, name: $name, link: $link"
        }
    }
}
