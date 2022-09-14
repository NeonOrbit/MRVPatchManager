package app.neonorbit.mrvpatchmanager.remote.data

import pl.droidsonroids.jspoon.annotation.Selector

class ApkPureItemLinkData {
    @Selector(value = "#download_link", attr = "href")
    lateinit var link: String

    @Selector(value = ".info-sdk > span")
    private var _versionName: String? = null

    val versionName: String? get() = _versionName?.takeIf { it.contains('.') }?.trim()
}
