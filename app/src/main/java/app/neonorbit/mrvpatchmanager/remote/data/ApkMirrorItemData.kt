package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.remote.ApkMirrorService
import app.neonorbit.mrvpatchmanager.util.Utils
import pl.droidsonroids.jspoon.annotation.Selector

class ApkMirrorItemData {
    @Selector(value = ".downloadButton", attr = "href")
    private lateinit var _link: String

    val link: String get() = Utils.absoluteUrl(
        ApkMirrorService.BASE_URL, _link
    ).let { "$it&forcebaseapk=true" }

    @Selector(value = ".appspec-value:contains(Version)")
    private var details: String? = null

    val versionName: String? by lazy {
        details?.let { ApkConfigs.extractVersionName(it) }
    }

    override fun toString(): String {
        return "versionName: $versionName, link: $link"
    }
}
