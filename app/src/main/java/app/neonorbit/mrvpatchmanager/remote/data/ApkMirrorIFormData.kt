package app.neonorbit.mrvpatchmanager.remote.data

import pl.droidsonroids.jspoon.annotation.Selector

class ApkMirrorIFormData {
    @Selector(value = "#filedownload", attr = "action")
    lateinit var action: String

    @Selector(value = "#filedownload > input", index = 0, attr = "value")
    lateinit var id: String

    @Selector(value = "#filedownload > input", index = 1, attr = "value")
    lateinit var key: String

    fun getLink(): String {
        return "$action?id=$id&key=$key&forcebaseapk=true"
    }
}
