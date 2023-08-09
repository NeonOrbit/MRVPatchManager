package app.neonorbit.mrvpatchmanager.repository.data

data class ApkFileData(val name: String, val path: String) {
    val version: String by lazy {
        "Version: " + name.substringAfterLast("-v").removeSuffix(".apk")
    }

    override fun toString(): String {
        return name
    }
}
