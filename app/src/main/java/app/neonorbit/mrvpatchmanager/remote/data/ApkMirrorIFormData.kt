package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.remote.ApkMirrorService
import app.neonorbit.mrvpatchmanager.util.Utils
import pl.droidsonroids.jspoon.annotation.Selector

class ApkMirrorIFormData {
    @Selector(value = "#filedownload", attr = "action")
    lateinit var action: String

    @Selector(value = "#filedownload > input", index = 0, attr = "value")
    lateinit var id: String

    @Selector(value = "#filedownload > input", index = 1, attr = "value")
    lateinit var key: String

    val link: String get() = Utils.absoluteUrl(
        ApkMirrorService.BASE_URL, "$action?id=$id&key=$key&forcebaseapk=true"
    )

    override fun toString(): String = "link: $link"
}
