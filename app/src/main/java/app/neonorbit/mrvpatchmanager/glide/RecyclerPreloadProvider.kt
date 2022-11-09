package app.neonorbit.mrvpatchmanager.glide

import android.graphics.drawable.Drawable
import androidx.fragment.app.Fragment
import app.neonorbit.mrvpatchmanager.repository.data.ApkFileData
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder

class RecyclerPreloadProvider(
    private val fragment: Fragment,
    private val paths: List<ApkFileData>
) : ListPreloader.PreloadModelProvider<ApkFileData> {
    override fun getPreloadItems(position: Int): List<ApkFileData> {
        return if (paths.isEmpty()) listOf() else listOf(paths[position])
    }

    override fun getPreloadRequestBuilder(item: ApkFileData): RequestBuilder<Drawable> {
        return Glide.with(fragment).load(item)
    }
}
