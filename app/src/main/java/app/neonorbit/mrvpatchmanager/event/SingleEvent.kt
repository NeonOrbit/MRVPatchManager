package app.neonorbit.mrvpatchmanager.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SingleEvent<T> : ChannelEvent<T> {
    private val channel = Channel<T>(Channel.CONFLATED)

    suspend fun post(event: T) {
        channel.send(event)
    }

    fun post(scope: CoroutineScope, event: T) {
        scope.launch {
            channel.send(event)
        }
    }

    override suspend fun observe(observer: FlowCollector<T>) {
        channel.receiveAsFlow().collect(observer)
    }
}
