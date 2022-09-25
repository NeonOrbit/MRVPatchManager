package app.neonorbit.mrvpatchmanager.event

sealed class UpdateEvent {
    data class Manager(val current: String, val latest: String, val link: String) : UpdateEvent()
    data class Module(val current: String, val latest: String, val link: String) : UpdateEvent()
}
