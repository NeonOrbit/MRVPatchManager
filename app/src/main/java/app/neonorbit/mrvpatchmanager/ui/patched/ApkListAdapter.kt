package app.neonorbit.mrvpatchmanager.ui.patched

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView
import app.neonorbit.mrvpatchmanager.R
import app.neonorbit.mrvpatchmanager.repository.data.ApkFileData
import app.neonorbit.mrvpatchmanager.ui.SelectionTrackerFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import java.util.Collections

class ApkListAdapter : RecyclerView.Adapter<ApkItemHolder>() {
    private var callback: Callback? = null
    private val list = ArrayList<ApkFileData>()
    private val viewHolders = HashSet<ApkItemHolder>()
    private lateinit var infoPreloader: ApkInfoPreloader
    private lateinit var tracker: SelectionTracker<Long>

    val items: List<ApkFileData> get() = Collections.unmodifiableList(list)

    init {
        setHasStableIds(true)
    }

    fun setItemClickListener(callback: Callback) {
        this.callback = callback
    }

    fun setApkInfoPreloader(preloader: ApkInfoPreloader) {
        this.infoPreloader = preloader
    }

    fun initTracker(recyclerView: RecyclerView): SelectionTracker<Long> {
        return SelectionTrackerFactory.buildFor(recyclerView).also {
            this.tracker = it
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApkItemHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.apk_item_view, parent, false)
        return ApkItemHolder(view)
    }

    override fun onBindViewHolder(holder: ApkItemHolder, position: Int) {
        viewHolders.add(holder)
        val item = list[position]
        holder.apkTitle.text = item.name
        infoPreloader.load(holder.apkInfo, position)
        Glide.with(holder.itemView)
            .load(item.path)
            .placeholder(R.drawable.generic_placeholder)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.apkIcon)
        callback?.let { call ->
            holder.itemView.setOnClickListener {
                call.onItemClicked(item)
            }
        }
        holder.itemView.isSelected = tracker.isSelected(holder.itemId)
    }

    override fun onViewRecycled(holder: ApkItemHolder) {
        super.onViewRecycled(holder)
        viewHolders.remove(holder)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun getItemIds(): List<Long> {
        return (0L until list.size).toList()
    }

    fun refresh() {
        viewHolders.forEach {
            it.itemView.isSelected = tracker.isSelected(it.itemId)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun reloadItems(new: List<ApkFileData>) {
        if (new == list) return
        list.clear()
        list.addAll(new)
        infoPreloader.reload()
        notifyDataSetChanged()
    }

    interface Callback {
        fun onItemClicked(item: ApkFileData)
    }
}
