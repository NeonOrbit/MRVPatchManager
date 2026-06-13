package app.neonorbit.mrvpatchmanager.event

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

class PendingEvent<T, R> : ChannelEvent<PendingEvent.Event<T, R>> {
    private var pending: Event<T, R>? = null
    private val mutex: Mutex = Mutex()
    private val channel = Channel<Event<T, R>>(Channel.CONFLATED)

    suspend fun waitForResult(data: T): R {
        return mutex.withLock {
            suspendCancellableCoroutine { continuation ->
                channel.trySend(Event<T, R>(data) {
                    pending = null
                    continuation.resume(it)
                }.also { pending = it })
            }
        }
    }

    fun sendResult(result: R) {
        pending?.callback?.invoke(result)
    }

    override suspend fun observe(observer: FlowCollector<Event<T, R>>) {
        channel.receiveAsFlow().collect(observer)
    }

    data class Event<T, R>(val data: T, val callback: (R) -> Unit)
}
