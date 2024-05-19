package app.neonorbit.mrvpatchmanager.download

import app.neonorbit.mrvpatchmanager.DefaultPreference
import app.neonorbit.mrvpatchmanager.network.HttpSpec
import app.neonorbit.mrvpatchmanager.parseJson
import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File

object DownloadCache {
    data class Cache(val eTag: String?, val mDate: String?, val length: Long)

    fun get(file: File): Cache? {
        return DefaultPreference.getString(file.asCacheKey())?.let {
            try { it.parseJson() } catch (_: Exception) { null }
        }
    }

    fun save(file: File, response: Response<ResponseBody>) {
        if (response.code() == HttpSpec.Code.PARTIAL_CONTENT) return
        Cache(
            response.headers()[HttpSpec.Header.E_TAG],
            response.headers()[HttpSpec.Header.LAST_MODIFIED],
            response.body()!!.contentLength()
        ).let {
            DefaultPreference.putString(file.asCacheKey(), Gson().toJson(it))
        }
    }

    private fun File.asCacheKey() = "download_cache_key_${name.lowercase()}"
}
