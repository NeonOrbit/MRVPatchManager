package app.neonorbit.mrvpatchmanager.event

import androidx.lifecycle.LifecycleOwner
import app.neonorbit.mrvpatchmanager.repeatOnUI
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.FlowCollector

interface ChannelEvent<T> {
    suspend fun observe(observer: FlowCollector<T>)

    fun observeOnUI(owner: LifecycleOwner, observer: FlowCollector<T>): Job {
        return owner.repeatOnUI { observe(observer) }
    }
}
