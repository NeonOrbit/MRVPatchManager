package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.remote.ApkMirrorService
import app.neonorbit.mrvpatchmanager.util.Utils
import pl.droidsonroids.jspoon.annotation.Selector

class ApkMirrorReleaseData {
    @Selector("#primary .listWidget:contains(Uploads) .appRow .appRowTitle")
    var releases: List<Release> = listOf()

    override fun toString(): String {
        return "releases: $releases"
    }

    class Release {
        @Selector("a", defValue = "")
        lateinit var name: String

        @Selector("a", attr = "href")
        private lateinit var href: String

        val version: String? get() = ApkConfigs.extractVersionName(name)

        val link: String get() = Utils.absoluteUrl(ApkMirrorService.BASE_URL, href)

        override fun toString(): String {
            return "name: $name, link: $link"
        }
    }
}
