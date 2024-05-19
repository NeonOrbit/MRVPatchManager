package app.neonorbit.mrvpatchmanager.download

import android.util.Log
import app.neonorbit.mrvpatchmanager.error
import app.neonorbit.mrvpatchmanager.lockOrThrow
import app.neonorbit.mrvpatchmanager.network.HttpSpec
import app.neonorbit.mrvpatchmanager.network.RetrofitClient
import app.neonorbit.mrvpatchmanager.useResponse
import app.neonorbit.mrvpatchmanager.util.RelaxedEmitter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import okhttp3.ResponseBody
import okio.buffer
import okio.sink
import retrofit2.Response
import java.io.File

object ApkDownloader {
    private val TAG = ApkDownloader::class.simpleName
    private const val SEGMENT_SIZE = 8_192L
    private const val INVALID_TYPE = "text/html"
    private val RANGE_REGEX by lazy { Regex("bytes ([0-9]*)-([0-9]*)/([0-9]*)") }

    private val mutex = Mutex()

    private fun buildHeader(file: File, cache: DownloadCache.Cache?): Map<String, String> {
        if (!file.exists() || cache == null) return mapOf()
        return HashMap<String, String>().apply {
            val length = file.length()
            if (length != cache.length) {
                put(HttpSpec.Header.RANGE, "bytes=$length-")
                put(HttpSpec.Header.IF_RANGE, cache.eTag ?: cache.mDate ?: "0")
            } else {
                cache.eTag?.let { put(HttpSpec.Header.IF_NONE_MATCH, it) }
                cache.mDate?.let { put(HttpSpec.Header.IF_MODIFIED_SINCE, it) }
            }
        }
    }

    fun download(url: String, file: File): Flow<DownloadStatus> = flow {
        mutex.lockOrThrow("A download is in progress.")
        try {
            emit(DownloadStatus.DOWNLOADING)
            val cache = DownloadCache.get(file)
            val headers = buildHeader(file, cache)
            RetrofitClient.SERVICE.download(url, headers).let {
                if (it.isInvalidRange(cache)) {
                    RetrofitClient.SERVICE.download(url)
                } else it
            }.takeUnless {
                HttpSpec.Code.NOT_MODIFIED == it.code()
            }?.useResponse {
                checkContentType(it)
                DownloadCache.save(file, it)
                emitAll(handleDownload(it, file))
            } ?: emit(DownloadStatus.FINISHED(file))
        } finally {
            mutex.unlock()
        }
    }

    private fun handleDownload(response: Response<ResponseBody>, file: File) = flow {
        val source = response.body()!!.source()
        val (start, limit) = getDownloadRange(response)

        val fresh = start < 1
        var totalRead = start
        if (fresh) file.delete()

        val relaxed = RelaxedEmitter(this, 50L)
        file.sink(!fresh).buffer().use { sink ->
            while (true) {
                source.read(sink.buffer, SEGMENT_SIZE).takeIf {
                    it != -1L
                }?.let { bytesRead ->
                    totalRead += bytesRead
                    sink.emitCompleteSegments()
                    relaxed.emit(DownloadStatus.PROGRESS(totalRead, limit))
                } ?: break
            }
        }
        relaxed.finish()
        emit(DownloadStatus.FINISHED(file))
    }.catch {
        Log.w(TAG, it)
        emit(DownloadStatus.FAILED(it.error))
    }.buffer(capacity = 12)

    private fun getDownloadRange(response: Response<ResponseBody>): Pair<Long, Long> {
        var start = 0L
        var limit = -1L
        if (response.code() == HttpSpec.Code.PARTIAL_CONTENT) {
            response.headers()[HttpSpec.Header.CONTENT_RANGE]?.let {
                RANGE_REGEX.find(it)
            }?.groupValues?.let {
                start = it[1].toLong()
                limit = it[2].toLong()
            } ?: throw AssertionError("Couldn't get download range")
        } else {
            limit = response.body()!!.contentLength() - 1
        }
        return Pair(start, limit)
    }

    private fun checkContentType(response: Response<ResponseBody>) {
        response.body()!!.contentType().toString().let { type ->
            if (type.contains(INVALID_TYPE)) throw Exception("Invalid content type")
        }
    }

    // ApkCombo is returning partial content despite ETag mismatch.
    private fun Response<ResponseBody>.isInvalidRange(cache: DownloadCache.Cache?): Boolean {
        if (this.code() == HttpSpec.Code.RANGE_NOT_SATISFIABLE) return true
        if (cache != null && this.code() == HttpSpec.Code.PARTIAL_CONTENT) {
            return this.headers()[HttpSpec.Header.E_TAG]?.takeIf { cache.eTag != null }?.let {
                cache.eTag != it
            } ?: this.headers()[HttpSpec.Header.LAST_MODIFIED]?.takeIf { cache.mDate != null }?.let {
                cache.mDate != it
            } ?: false
        }
        return false
    }
}
