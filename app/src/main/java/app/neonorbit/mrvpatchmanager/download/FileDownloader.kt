package app.neonorbit.mrvpatchmanager.download

import android.util.Log
import app.neonorbit.mrvpatchmanager.DefaultPreference
import app.neonorbit.mrvpatchmanager.error
import app.neonorbit.mrvpatchmanager.network.HttpSpec
import app.neonorbit.mrvpatchmanager.network.RetrofitClient
import app.neonorbit.mrvpatchmanager.parseJson
import app.neonorbit.mrvpatchmanager.useResponse
import app.neonorbit.mrvpatchmanager.util.RelaxedEmitter
import com.google.gson.Gson
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
import java.util.regex.Pattern

object FileDownloader {
    private val TAG = FileDownloader::class.simpleName
    private const val SEGMENT_SIZE = 8_192L
    private const val INVALID_TYPE = "text/html"
    private const val RANGE_PATTERN = "bytes ([0-9]*)-([0-9]*)/([0-9]*)"

    private val mutex = Mutex()

    private fun buildHeader(file: File): Map<String, String> {
        val info = getDownloadInfo(file)
        val headers = HashMap<String, String>()
        if (file.exists() && info != null) {
            val length = file.length()
            if (length != info.cLength) {
                headers[HttpSpec.Header.RANGE] = "bytes=$length-"
                headers[HttpSpec.Header.IF_RANGE] = info.eTag ?: info.mDate ?: "0"
            } else {
                info.eTag?.let { headers[HttpSpec.Header.IF_NONE_MATCH] = it }
                info.mDate?.let { headers[HttpSpec.Header.IF_MODIFIED_SINCE] = it }
            }
        }
        return headers
    }

    fun download(url: String, file: File): Flow<DownloadStatus> = flow {
        if (!mutex.tryLock()) {
            throw IllegalStateException("A download is in progress.")
        }
        try {
            val headers = buildHeader(file)
            emit(DownloadStatus.DOWNLOADING)
            RetrofitClient.SERVICE.download(url, headers).let {
                if (it.code() == HttpSpec.Code.RANGE_NOT_SATISFIABLE) {
                    RetrofitClient.SERVICE.download(url)
                } else it
            }.also {
                if (it.code() == HttpSpec.Code.NOT_MODIFIED) {
                    emit(DownloadStatus.FINISHED(file))
                    return@flow
                }
            }.useResponse { response ->
                checkContentType(response)
                saveDownloadInfo(file, response)
                emitAll(handleDownload(response, file))
            }
        } finally {
            mutex.unlock()
        }
    }

    private fun handleDownload(response: Response<ResponseBody>, file: File) = flow {
        val source = response.body()!!.source()
        val (start, length) = getDownloadRange(response)

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
                    relaxed.emit(DownloadStatus.PROGRESS(totalRead, length))
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
        var startingByte = 0L
        var contentLength = -1L
        if (response.code() == HttpSpec.Code.PARTIAL_CONTENT) {
            response.headers()[HttpSpec.Header.CONTENT_RANGE]?.let {
                Pattern.compile(RANGE_PATTERN).matcher(it)
            }?.takeIf { it.find() }?.let {
                startingByte = it.group(1)!!.toLong()
                contentLength = it.group(2)!!.toLong()
            } ?: throw AssertionError("Couldn't get download range")
        } else {
            contentLength = response.body()!!.contentLength()
        }
        return Pair(startingByte, contentLength)
    }

    private fun checkContentType(response: Response<ResponseBody>) {
        response.body()!!.contentType().toString().let { type ->
            if (type.contains(INVALID_TYPE)) throw Exception("Invalid content type")
        }
    }

    private fun getInfoKey(file: File) = "download_info_${file.name.lowercase()}"

    private fun getDownloadInfo(file: File): DownloadInfo? {
        return DefaultPreference.getString(getInfoKey(file))?.let {
            try { it.parseJson() } catch (_: Exception) { null }
        }
    }

    private fun saveDownloadInfo(file: File, response: Response<ResponseBody>) {
        DownloadInfo(
            response.headers()[HttpSpec.Header.E_TAG],
            response.headers()[HttpSpec.Header.LAST_MODIFIED],
            response.body()!!.contentLength()
        ).let {
            DefaultPreference.setString(getInfoKey(file), Gson().toJson(it))
        }
    }

    data class DownloadInfo(val eTag: String?, val mDate: String?, val cLength: Long)
}
