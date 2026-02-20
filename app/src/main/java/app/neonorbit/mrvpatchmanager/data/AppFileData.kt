package app.neonorbit.mrvpatchmanager.data

import java.io.File

data class AppFileData(val name: String, val base: File, val splits: List<File>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AppFileData
        if (name != other.name) return false
        if (base != other.base) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + base.hashCode()
        return result
    }
}