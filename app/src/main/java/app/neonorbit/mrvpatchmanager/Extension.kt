package app.neonorbit.mrvpatchmanager

import retrofit2.HttpException
import retrofit2.Response
import java.io.Closeable
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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

val Throwable.error: String; get() = this.message ?: this.javaClass.simpleName

val Throwable.isConnectError: Boolean; get() {
    return this is ConnectException|| this is UnknownHostException|| this is SocketTimeoutException
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
