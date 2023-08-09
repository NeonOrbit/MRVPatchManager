package app.neonorbit.mrvpatchmanager.apk

import java.io.File
import java.util.zip.ZipFile

object ApkParser {
    fun getABI(file: File) = ZipFile(file).use { zip ->
        val libs = mutableListOf<String>()
        for (entry in zip.entries()) {
            if (entry.name.startsWith("lib/")) {
                libs.add(entry.name)
            } else if (libs.isNotEmpty()) break
        }
        when {
            libs.any { ApkConfigs.ARM_64 in it } -> ApkConfigs.ARM_64
            libs.any { ApkConfigs.ARM_32 in it } -> ApkConfigs.ARM_32
            libs.any { ApkConfigs.X86_64 in it } -> ApkConfigs.X86_64
            libs.any { ApkConfigs.X86 in it } -> ApkConfigs.X86
            else -> null
        }
    }
}
