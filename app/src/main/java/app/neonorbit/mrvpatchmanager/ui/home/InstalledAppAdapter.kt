package app.neonorbit.mrvpatchmanager.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.neonorbit.mrvpatchmanager.R
import app.neonorbit.mrvpatchmanager.data.AppFileData
import app.neonorbit.mrvpatchmanager.ui.home.InstalledAppAdapter.ItemHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class InstalledAppAdapter(private val list: List<AppFileData>) : RecyclerView.Adapter<ItemHolder>() {
    private lateinit var callback: (AppFileData) -> Unit

    fun setItemClickListener(callback: (AppFileData) -> Unit) {
        this.callback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.apk_simple_item_view, parent, false)
        return ItemHolder(view)
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val item = list[position]
        holder.apkTitle.text = item.name
        Glide.with(holder.itemView)
            .load(item.base.absolutePath)
            .placeholder(R.drawable.generic_placeholder)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.apkIcon)
        holder.itemView.setOnClickListener {
            callback(item)
        }
    }

    override fun getItemCount() = list.size

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val apkIcon: ImageView = itemView.findViewById(R.id.apk_icon)
        val apkTitle: TextView = itemView.findViewById(R.id.apk_title)
    }
}
