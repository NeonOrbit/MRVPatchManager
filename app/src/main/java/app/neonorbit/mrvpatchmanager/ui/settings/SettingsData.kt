package app.neonorbit.mrvpatchmanager.ui.settings

import app.neonorbit.mrvpatchmanager.remote.ApkRemoteFileProvider

object SettingsData {
    const val DEFAULT_SERVER = "All servers"

    val SERVERS by lazy {
        ApkRemoteFileProvider.services.map { it.server() }.apply {
            (this as MutableList<String>).add(0, DEFAULT_SERVER)
        }.toTypedArray()
    }
}