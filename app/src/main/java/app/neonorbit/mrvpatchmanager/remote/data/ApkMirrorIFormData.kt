package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.remote.ApkMirrorService
import app.neonorbit.mrvpatchmanager.util.Utils
import pl.droidsonroids.jspoon.annotation.Selector

class ApkMirrorIFormData {
    @Selector(value = "#download-link", attr = "href")
    private var href: String? = null

    @Selector(value = "#filedownload", attr = "action")
    private var action: String? = null

    @Selector(value = "#filedownload > input[name=id]", attr = "value")
    private var id: String? = null

    @Selector(value = "#filedownload > input[name=key]", attr = "value")
    private var key: String? = null

    val link: String get() = Utils.absoluteUrl(ApkMirrorService.BASE_URL,
        href ?: "$action?id=$id&key=$key&forcebaseapk=true"
    )

    override fun toString(): String = "link: $link"
}
