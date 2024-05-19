package app.neonorbit.mrvpatchmanager.data

sealed class UpdateEventData {
    data class Manager(val current: String, val latest: String, val link: String) : UpdateEventData()
    data class Module(val current: String, val latest: String, val link: String) : UpdateEventData()
}
