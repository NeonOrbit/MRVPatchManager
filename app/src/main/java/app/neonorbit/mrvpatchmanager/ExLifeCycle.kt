package app.neonorbit.mrvpatchmanager

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun LifecycleOwner.repeatOnUI(
    block: suspend CoroutineScope.() -> Unit
) = lifecycleScope.launch(Dispatchers.Main.immediate) {
    repeatOnLifecycle(Lifecycle.State.STARTED, block)
}

fun <T> Flow<T>.observeOnUI(
    owner: LifecycleOwner,
    collector: FlowCollector<T>
) = owner.repeatOnUI { this@observeOnUI.collect(collector) }

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
