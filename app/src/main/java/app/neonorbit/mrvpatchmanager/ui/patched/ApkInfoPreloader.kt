package app.neonorbit.mrvpatchmanager.ui.patched

import android.widget.TextView
import androidx.annotation.UiThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import app.neonorbit.mrvpatchmanager.removeFirstIf
import app.neonorbit.mrvpatchmanager.repository.data.ApkFileData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

class ApkInfoPreloader(private val lifecycleOwner: LifecycleOwner,
                       private val maxPreload: Int,
                       private val dataSource: List<ApkFileData>) {
    private val mutex: Mutex = Mutex()
    private val items = ArrayList<ApkFileData>()
    private val cached = ConcurrentHashMap<Int, String>()
    private val requests: WeakHashMap<TextView, Int> = WeakHashMap()
    private val lifecycleScope get() = lifecycleOwner.lifecycleScope

    private fun nextPreloadRange(): IntRange {
        return cached.size until (cached.size + maxPreload).coerceAtMost(items.size)
    }

    @Volatile
    private var onReload = false
    private var preloadJob: Job? = null

    @UiThread
    fun load(view: TextView, position: Int) {
        cached[position]?.takeIf { !onReload }?.let {
            view.text = it
        } ?: enqueue(view, position)
    }

    @UiThread
    fun reload() {
        if (onReload) return
        else onReload = true
        requests.clear()
        preloadJob?.cancel()
        lifecycleScope.launch(Dispatchers.Main.immediate) {
            mutex.withLock {
                items.clear()
                cached.clear()
                items.addAll(dataSource)
            }
            triggerPreload()
            onReload = false
        }
    }

    private fun enqueue(view: TextView, position: Int) {
        view.placeholder(position)
        requests[view] = position
        if (!onReload) triggerPreload()
    }

    private fun triggerPreload() {
        if (!mutex.tryLock()) return
        lifecycleScope.launch (Dispatchers.IO) {
            preloadAndUpdate()
        }.also {
            preloadJob = it
        }.invokeOnCompletion {
            preloadJob = null
            mutex.unlock()
            if (requests.isNotEmpty() && cached.size < items.size) {
                triggerPreload()
            }
        }
    }

    private suspend fun preloadAndUpdate() {
        val jobs = ArrayList<Job>(maxPreload)
        for (position in nextPreloadRange()) {
            val path = items[position].path
            coroutineContext.ensureActive()
            lifecycleScope.launch(coroutineContext) {
                ApkUtil.getApkSummery(File(path)).let { info ->
                    cached[position] = (info ?: items[position].version).also {
                        updateViewFor(position, it)
                    }
                }
            }.also { jobs.add(it) }
        }
        jobs.joinAll()
        updateRequestedViews()
    }

    private suspend fun updateRequestedViews() = runOnUI {
        requests.entries.removeIf { request ->
            cached[request.value]?.let { info ->
                request.key?.text = info
            } != null
        }
    }

    private suspend fun updateViewFor(position: Int, info: String) = runOnUI {
        requests.entries.removeFirstIf { request ->
            request.takeIf { it.value == position }?.let {
                it.key?.text = info
            } != null
        }
    }

    private fun TextView.placeholder(position: Int) {
        text = items.getOrNull(position)?.version
    }

    private suspend fun runOnUI(block: suspend () -> Unit) {
        if (lifecycleOwner.isCreated) {
            withContext(Dispatchers.Main.immediate) {
                if (lifecycleOwner.isCreated) {
                    block()
                }
            }
        }
    }

    private val LifecycleOwner.isCreated get() = lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)
}
