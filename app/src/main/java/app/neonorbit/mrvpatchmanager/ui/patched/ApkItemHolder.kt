package app.neonorbit.mrvpatchmanager.ui.patched

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import app.neonorbit.mrvpatchmanager.R
import app.neonorbit.mrvpatchmanager.ui.SelectionTrackerFactory

class ApkItemHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView),
    SelectionTrackerFactory.TrackerItemDetails
{
    val apkIcon: ImageView = itemView.findViewById(R.id.apk_icon)
    val apkTitle: TextView = itemView.findViewById(R.id.apk_title)
    val apkInfo: TextView = itemView.findViewById(R.id.apk_info)

    override fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> {
        return object: ItemDetailsLookup.ItemDetails<Long>() {
            override fun getPosition(): Int {
                return bindingAdapterPosition
            }
            override fun getSelectionKey(): Long {
                return itemId
            }
        }
    }
}
