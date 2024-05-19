package app.neonorbit.mrvpatchmanager.util

import kotlinx.coroutines.flow.FlowCollector

class RelaxedEmitter<in T>(
    private val collector: FlowCollector<T>,
    private val interval: Long
) {
    private var skipped: T? = null
    private var emittedAt: Long = 0L

    suspend fun emit(value: T) {
        if (skip()) {
            skipped = value
        } else {
            skipped = null
            collector.emit(value)
            emittedAt = System.currentTimeMillis()
        }
    }

    suspend fun finish() {
        skipped?.let { collector.emit(it) }
    }

    private fun skip(): Boolean {
        return (interval - (System.currentTimeMillis() - emittedAt)) > 0
    }
}
