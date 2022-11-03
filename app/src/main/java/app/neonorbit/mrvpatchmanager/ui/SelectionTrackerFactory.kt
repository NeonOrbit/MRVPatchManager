package app.neonorbit.mrvpatchmanager.ui

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.OperationMonitor
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StableIdKeyProvider
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.RecyclerView

object SelectionTrackerFactory {
    fun buildFor(recyclerView: RecyclerView): SelectionTracker<Long> {
        val monitor = OperationMonitor()
        return SelectionTracker.Builder(
            recyclerView.javaClass.name,
            recyclerView,
            StableIdKeyProvider(recyclerView),
            SelectionItemLookup(recyclerView),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        ).withOperationMonitor(
            monitor
        ).withSelectionPredicate(object : SelectionTracker.SelectionPredicate<Long>() {
            override fun canSelectMultiple(): Boolean = true
            override fun canSetStateForKey(k: Long, n:Boolean): Boolean = !monitor.isStarted
            override fun canSetStateAtPosition(p: Int, n: Boolean): Boolean = !monitor.isStarted
        }).build()
    }

    class SelectionItemLookup(private val recyclerView: RecyclerView) : ItemDetailsLookup<Long>() {
        override fun getItemDetails(event: MotionEvent): ItemDetails<Long>? {
            return recyclerView.findChildViewUnder(event.x, event.y)?.let {
                (recyclerView.getChildViewHolder(it) as TrackerItemDetails).getItemDetails()
            }
        }
    }

    interface TrackerItemDetails {
        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long>
    }
}
