package app.neonorbit.mrvpatchmanager.local

import app.neonorbit.mrvpatchmanager.AppConfig
import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import app.neonorbit.mrvpatchmanager.repository.data.ApkFileData
import java.io.File
import java.util.stream.Collectors

class ApkLocalFileProvider {
    fun getModuleApk(): File {
        return File(AppConfig.DOWNLOAD_DIR, AppConfig.MODULE_APK_NAME).also { file ->
            AppServices.assetManager.open(AppConfig.MODULE_ASSET_NAME).use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    fun loadPatchedApks(): List<ApkFileData> {
        return AppConfig.PATCHED_APK_DIR.listFiles()?.associateBy(
            {it.toApkData()}, {it.lastModified()}
        )?.entries?.stream()?.sorted(
            Comparator.comparing<Map.Entry<*, Long>, Long> { it.value }.reversed()
        )?.map { it.key }?.collect(Collectors.toList()) ?: listOf()
    }

    private fun File.toApkData(): ApkFileData = ApkFileData(
        name, absolutePath, ApkUtil.getApkVersionName(this) ?: "unknown"
    )
}
