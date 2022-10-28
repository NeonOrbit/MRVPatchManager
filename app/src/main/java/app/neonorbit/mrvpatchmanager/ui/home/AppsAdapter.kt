package app.neonorbit.mrvpatchmanager.ui.home

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import androidx.core.content.ContextCompat
import app.neonorbit.mrvpatchmanager.R
import app.neonorbit.mrvpatchmanager.data.AppItemData

class AppsAdapter(context: Context, items: List<AppItemData>)
    : ArrayAdapter<AppItemData>(context, R.layout.dropdown_item, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getView(position, convertView, parent).also {
            (it as TextView).setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(context, getItem(position)!!.icon), null, null, null
            )
        }
    }

    private val noOpFilter = object : Filter() {
        private val noOpResult = FilterResults()
        override fun performFiltering(constraint: CharSequence?) = noOpResult
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {}
    }

    override fun getFilter() = noOpFilter
}
