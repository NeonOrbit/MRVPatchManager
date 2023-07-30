package app.neonorbit.mrvpatchmanager

import android.content.Context
import android.net.Uri
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import retrofit2.Response
import java.io.Closeable
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.log10
import kotlin.math.pow

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

fun File.size(): Long {
    return if (isFile) length()
    else listFiles()?.sumOf { sub ->
        sub.size()
    } ?: 0L
}

fun Uri.toTempFile(): File = File.createTempFile(
    "resolved", null, AppConfig.TEMP_DIR
).let { copyTo(it) }

fun Uri.copyTo(file: File): File {
    AppServices.contentResolver.openInputStream(this)?.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: throw Exception("Failed to resolve uri: $this")
    return file
}

fun List<File>.existAnyIn(dir: DocumentFile): Boolean = any { file ->
    dir.listFiles().any { file.name.equals(it.name, true) }
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
        else -> this.error
    }.take(length).let {
        if (it.length > length) "${it}..." else it
    }
}

fun LifecycleOwner.withLifecycle(
    state: Lifecycle.State,
    block: suspend CoroutineScope.() -> Unit
) = lifecycleScope.launch(Dispatchers.Main.immediate) {
    repeatOnLifecycle(state, block)
}

fun <T> Flow<T>.observeOnUI(
    owner: LifecycleOwner,
    collector: FlowCollector<T>
) = owner.lifecycleScope.launch(Dispatchers.Main.immediate) {
    owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        this@observeOnUI.collect(collector)
    }
}

fun <T> MutableStateFlow<T>.post(
    value: T,
    with: ViewModel
) = with.viewModelScope.launch { emit(value) }

fun <T> MutableStateFlow<T>.postNow(
    value: T,
    with: ViewModel
) = with.viewModelScope.launch(Dispatchers.Main.immediate) { emit(value) }

/**
 * Launches a coroutine and immediately returns the Job,
 * then executes the given action under the mutex's lock.
 */
fun CoroutineScope.launchSyncedBlock(
    mutex: Mutex,
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return this.launch(context) {
        mutex.withLock {
            block()
        }
    }
}

fun Long.toSize(withSpace: Boolean = false): String {
    val space = (if (withSpace) " " else "")
    if (this <= 0) return "0${space}Bytes"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digit = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return String.format("%.2f", (this.toDouble() / 1024.0.pow(digit))).let {
        "$it${space}${units[digit]}"
    }
}

fun String.compareVersion(other: String?): Int {
    if (other?.isEmpty() != false) return 1
    return try {
        val v1 = this.trimStart('v').split('.')
        val v2 = other.trimStart('v').split('.')
        check(v1.size == v2.size)
        for (i in v1.indices) {
            val n1 = v1[i].toInt()
            val n2 = v2[i].toInt()
            if (n1 != n2) {
                return n1.compareTo(n2)
            }
        }
        0
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

@Suppress("unused")
fun String.capitalizeWords(): String = split(" ").joinToString(" ") {
    it.lowercase(Locale.getDefault()).replaceFirstChar { word ->
        if (word.isLowerCase()) word.titlecase(Locale.getDefault()) else it
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
