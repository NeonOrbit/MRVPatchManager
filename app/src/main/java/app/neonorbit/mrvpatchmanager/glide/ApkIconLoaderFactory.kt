package app.neonorbit.mrvpatchmanager.glide

import android.content.Context
import android.graphics.drawable.Drawable
import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import java.io.File

class ApkIconLoaderFactory(val context: Context) : ModelLoaderFactory<String, Drawable> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<String, Drawable> {
        return ApkIconModelLoader()
    }

    override fun teardown() {}

    class ApkIconModelLoader : ModelLoader<String, Drawable> {
        override fun buildLoadData(
            model: String,
            width: Int,
            height: Int,
            options: Options
        ): ModelLoader.LoadData<Drawable> {
            return ModelLoader.LoadData(ObjectKey(model), ApkIconFetcher(model))
        }

        override fun handles(model: String): Boolean {
            return model.substringAfterLast(".").contains("apk", true)
        }
    }

    class ApkIconFetcher(private val path: String) : DataFetcher<Drawable> {
        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Drawable>) {
            ApkUtil.getApkIcon(File(path)).let { icon ->
                callback.onDataReady(icon)
            }
        }

        override fun getDataSource(): DataSource = DataSource.LOCAL

        override fun getDataClass(): Class<Drawable> = Drawable::class.java

        override fun cancel() {}

        override fun cleanup() {}
    }
}
