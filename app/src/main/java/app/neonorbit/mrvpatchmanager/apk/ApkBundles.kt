package app.neonorbit.mrvpatchmanager.apk

import android.graphics.drawable.Drawable
import app.neonorbit.mrvpatchmanager.AppConfigs
import com.google.gson.JsonParser
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ApkBundles {
    private const val ANDROID_MANIFEST = ApkConfigs.ANDROID_MANIFEST

    fun <T> peekBaseApkFromBundle(bundle: File, base: (File?)-> T): T {
        var temp: File? = null
        try {
            temp = getBaseApkFromBundle(bundle)
            return base(temp)
        } finally { temp?.delete() }
    }

    fun getBaseApkFromBundle(bundle: File): File? {
        return ZipFile(bundle).use { zip ->
            if (zip.getEntry(ANDROID_MANIFEST) != null) return null
            val base = try {
                zip.getEntry("manifest.json")?.open(zip)?.let { stream ->
                    val json = JsonParser.parseReader(InputStreamReader(stream))
                    json.getAsJsonObject()?.getAsJsonArray("split_apks")?.firstOrNull {
                        it.getAsJsonObject()?.get("id")?.asString == "base"
                    }?.getAsJsonObject()?.get("file")?.asString?.takeIf { it.endsWith(".apk") }.let {
                        zip.getEntry(it)
                    }
                }
            } catch (_: Throwable) {null} ?: zip.getEntry("base.apk")
            val baseApk = File(AppConfigs.TEMP_DIR, "base_${base.crc}.apk")
            if (baseApk.length() != base.size) {
                check(!baseApk.exists() || baseApk.delete())
                base.open(zip).use { stream ->
                    BufferedOutputStream(FileOutputStream(baseApk), 128 * 1024).use { os ->
                        val buffer = ByteArray(128 * 1024)
                        var len: Int
                        while ((stream.read(buffer).also { len = it }) != -1) {
                            os.write(buffer, 0, len)
                        }
                    }
                }
                if (baseApk.length() != base.size) {
                    baseApk.delete()
                    throw Exception("Base apk extraction failed")
                }
            }
            baseApk
        }
    }

    fun createTempBundle(base: File, splits: List<File>): File {
        return File.createTempFile("bundle", ".apks", AppConfigs.TEMP_DIR).also { out ->
            ZipOutputStream(BufferedOutputStream(FileOutputStream(out), 512 * 1024)).use { zos ->
                zos.setLevel(Deflater.NO_COMPRESSION)
                (listOf(base) + splits).forEach { file ->
                    val entry = ZipEntry(if (file === base) "base.apk" else file.name)
                    zos.putNextEntry(entry)
                    file.inputStream().use { it.copyTo(zos, 128 * 1024) }
                    zos.closeEntry()
                }
            }
        }
    }

    fun getIconFromBundle(bundle: File): Drawable? {
        return try {
            ZipFile(bundle).use { zip ->
                if (zip.getEntry(ANDROID_MANIFEST) != null) return null
                return zip.getEntry("icon.png")?.open(zip)?.use {
                    Drawable.createFromStream(it, "icon")
                }
            }
        } catch (_: Exception) { null }
    }

    fun getSummeryFromBundle(bundle: File): String? {
        return try {
            ZipFile(bundle).use { zip ->
                if (zip.getEntry(ANDROID_MANIFEST) != null) return null
                var arm64 = false; var armabi = false; var vercode: String? = null
                val masked = zip.getEntry(AppConfigs.BUNDLE_MASKED_MARKER) != null
                val jsonEntry = zip.getEntry("manifest.json") ?: zip.getEntry("info.json")
                jsonEntry?.open(zip)?.let { stream ->
                    val json = JsonParser.parseReader(InputStreamReader(stream)).asJsonObject
                    vercode = (json?.get("versioncode") ?: json?.get("version_code"))?.asString
                    (json?.get("arches") ?: json?.get("split_apks"))?.toString()?.let {
                        arm64 = it.contains("arm64")
                        armabi = it.contains("armabi")
                    }
                }
                return if (vercode == null || (!arm64 && !armabi)) null
                else "$vercode (${if (arm64) "arm64-v8a" else "armeabi-v7a"})" + (if (masked) " [masked]" else "")
            }
        } catch (_: Exception) { null }
    }

    private fun ZipEntry.open(zip: ZipFile) = zip.getInputStream(this)
}
