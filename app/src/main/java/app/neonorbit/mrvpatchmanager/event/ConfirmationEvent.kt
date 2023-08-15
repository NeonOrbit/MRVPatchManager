package app.neonorbit.mrvpatchmanager.event

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

class ConfirmationEvent : ChannelEvent<ConfirmationEvent.Event> {
    private var pending: Event? = null
    private val mutex: Mutex = Mutex()
    private val channel = Channel<Event>(Channel.CONFLATED)

    suspend fun ask(msg: String): Boolean {
        return ask(null, msg)
    }

    suspend fun ask(title: String?, msg: String): Boolean {
        return ask(title, msg, null)
    }

    suspend fun ask(title: String? = null, msg: String, action: String? = null): Boolean {
        return mutex.withLock {
            suspendCancellableCoroutine { continuation ->
                channel.trySend(Event(title, msg, action) {
                    pending = null
                    continuation.resume(it)
                }.also { pending = it })
            }
        }
    }

    fun sendResponse(result: Boolean) {
        pending?.response?.invoke(result)
    }

    override suspend fun observe(observer: FlowCollector<Event>) {
        channel.receiveAsFlow().collect(observer)
    }

    data class Event(val title: String?, val message: String, val action: String?, val response: (Boolean) -> Unit)
}
