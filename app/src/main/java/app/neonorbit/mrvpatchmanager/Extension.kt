package app.neonorbit.mrvpatchmanager

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import androidx.documentfile.provider.DocumentFile
import app.neonorbit.mrvpatchmanager.network.HttpSpec
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import java.io.Closeable
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

fun Mutex.lockOrThrow(msg: String) {
    if (!tryLock()) throw IllegalStateException(msg)
}

inline fun <reified T> String.parseJson(): T? {
    return Gson().fromJson(this, object : TypeToken<T>() {}.type)
}

fun <T> Response<T>.response(): Response<T> {
    if (!isSuccessful) {
        throw HttpException(this)
    }
    return this
}

fun <T> Response<T>.result(): T {
    return response().body()!!
}

inline fun <T> Response<T>.useResponse(block: (Response<T>) -> Unit) {
    try {
        block(response())
    } finally {
        this.body()?.let {
            if (it is Closeable) it.close()
        }
    }
}

fun List<File>.existAnyIn(dir: DocumentFile): Boolean = dir.listFiles().let { all ->
    this.any { file -> all.any { file.name.equals(it.name, true) } }
}

fun File.totalSize(): Long {
    return if (isFile) length()
    else listFiles()?.sumOf { sub ->
        sub.totalSize()
    } ?: 0L
}

fun Long.toSizeString(withSpace: Boolean = false): String {
    val space = (if (withSpace) " " else "")
    if (this <= 0) return "0${space}Bytes"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digit = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return String.format(Locale.ROOT, "%.2f", (this.toDouble() / 1024.0.pow(digit))).let {
        "$it${space}${units[digit]}"
    }
}

fun Uri.toTempFile(resolver: ContentResolver): File = File.createTempFile(
    "resolved", null, AppConfigs.TEMP_DIR
).let { copyTo(resolver, it) }

fun Uri.copyTo(resolver: ContentResolver, dest: File): File {
    resolver.openInputStream(this)?.use { input ->
        dest.outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: throw Exception("Failed to resolve uri: $this")
    return dest
}

val Throwable.error: String; get() = this.message ?: this.javaClass.simpleName

val Throwable.isConnectError: Boolean; get() = when (this) {
    is ConnectException, is UnknownHostException, is SocketTimeoutException -> true
    else -> false
}

fun Throwable.toNetworkError(isOnline: Boolean, length: Int = 100): String {
    return when {
        !isOnline -> "No internet connection"
        isConnectError -> "Couldn't connect to the server"
        this is HttpException -> this.httpError
        else -> this.error
    }.take(length).let {
        if (it.length > length) "${it}..." else it
    }
}

suspend fun Throwable.toNetworkError(length: Int = 100) = withContext(Dispatchers.IO) {
    toNetworkError(try { AppServices.isNetworkOnline() } catch (e: Exception) { false }, length)
}

val HttpException.httpError: String; get() = when (code()) {
    HttpSpec.Code.TOO_MANY_REQUESTS -> "Too Many Requests [Please try again later]"
    else -> error
}

fun String.compareVersion(other: String?): Int {
    if (this == other) return 0
    if (other?.isEmpty() != false) return 1
    return try {
        val v1 = this.trimStart('v').split('.')
        val v2 = other.trimStart('v').split('.')
        for (i in 0..(v1.size.coerceAtMost(v2.size))) {
            val n1 = v1[i].toInt()
            val n2 = v2[i].toInt()
            if (n1 != n2) {
                return n1.compareTo(n2)
            }
        }
        v1.size - v2.size
    } catch (_: Exception) { -1 }
}

fun String?.isValidJavaName(): Boolean {
    if (this == null) return false
    for (part in this.split('.').toTypedArray()) {
        if (part.isEmpty()) return false
        if (!Character.isJavaIdentifierStart(part[0])) return false
        if (part.any { !Character.isJavaIdentifierPart(it) }) return false
    }
    return true
}

fun <T> MutableCollection<T>.removeFirstIf(predicate: (T) -> Boolean) {
    this.iterator().let {
        while (it.hasNext()) if (predicate(it.next())) {
            it.remove()
            return
        }
    }
}

@Suppress("unused")
fun String.capitalizeWords(): String = split(" ").joinToString(" ") {
    it.lowercase().replaceFirstChar { word ->
        if (word.isLowerCase()) word.uppercase() else it
    }
}

fun TextView.setLinkedText(@StringRes resId: Int, linkText: String, linkUrl: String) {
    text = context.getStringWithLink(resId, linkText, linkUrl)
    movementMethod = LinkMovementMethod.getInstance()
}

fun Context.getStringWithLink(@StringRes resId: Int, text: String, url: String): Spanned {
    return HtmlCompat.fromHtml(getString(
        resId, "<a href=\"$url\">$text</a>"), HtmlCompat.FROM_HTML_MODE_COMPACT
    )
}
