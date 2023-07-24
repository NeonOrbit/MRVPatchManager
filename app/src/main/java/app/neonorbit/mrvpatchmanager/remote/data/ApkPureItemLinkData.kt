package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.remote.ApkPureService
import app.neonorbit.mrvpatchmanager.util.Utils
import pl.droidsonroids.jspoon.annotation.Selector

class ApkPureItemLinkData {
    @Selector(value = "#download_link", attr = "href")
    private lateinit var _link: String

    val link: String get() = Utils.absoluteUrl(ApkPureService.BASE_URL, _link)

    @Selector(value = ".info-sdk > span")
    private var _versionName: String? = null

    val versionName: String? get() = _versionName?.takeIf { it.contains('.') }?.trim()

    override fun toString(): String = "versionName: $versionName, link: $link"
}
