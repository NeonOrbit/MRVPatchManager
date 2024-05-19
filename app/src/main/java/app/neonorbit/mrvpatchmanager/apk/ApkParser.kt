package app.neonorbit.mrvpatchmanager.apk

import app.neonorbit.mrvpatchmanager.AppConfigs
import app.neonorbit.mrvpatchmanager.error
import app.neonorbit.mrvpatchmanager.util.Utils
import com.google.gson.Gson
import org.lsposed.lspatch.share.PatchConfig
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

object ApkParser {
    fun getABI(file: File): String? = try {
        ZipFile(file).use { zip ->
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
    } catch (e: Exception) {
        Utils.warn(e.error, e)
        null
    }

    fun getPatchedConfig(file: File): PatchConfig? = try {
        ZipFile(file).use { zip ->
            zip.getInputStream(zip.getEntry(AppConfigs.PATCHED_APK_CONFIG_PATH)).use { input ->
                Gson().fromJson(input.bufferedReader(StandardCharsets.UTF_8), PatchConfig::class.java)
            }
        }
    } catch (e: Exception) {
        Utils.warn(e.error, e)
        null
    }
}
