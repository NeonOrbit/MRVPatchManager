package app.neonorbit.mrvpatchmanager.repository.data

data class ApkFileData(val name: String,
                       val path: String,
                       val version: String) {
    override fun toString(): String {
        return name
    }
}
