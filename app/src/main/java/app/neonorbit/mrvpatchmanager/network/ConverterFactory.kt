package app.neonorbit.mrvpatchmanager.network

import app.neonorbit.mrvpatchmanager.network.marker.HtmlMarker
import app.neonorbit.mrvpatchmanager.network.marker.JsonMarker
import app.neonorbit.mrvpatchmanager.network.marker.XmlMarker
import okhttp3.ResponseBody
import pl.droidsonroids.retrofit2.JspoonConverterFactory
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type

class ConverterFactory : Converter.Factory() {
    companion object {
        val PARSERS = listOf(
            XmlMarker::class, HtmlMarker::class, JsonMarker::class
        )
    }

    private val gson: GsonConverterFactory by lazy {
        GsonConverterFactory.create()
    }

    private val html: JspoonConverterFactory by lazy {
        JspoonConverterFactory.create()
    }

    @Suppress("deprecation")
    private val xml: retrofit2.converter.simplexml.SimpleXmlConverterFactory by lazy {
        retrofit2.converter.simplexml.SimpleXmlConverterFactory.create()
    }

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        return annotations.firstOrNull {
            it.annotationClass in PARSERS
        }?.annotationClass?.let { parser ->
            return when (parser) {
                XmlMarker::class -> xml.responseBodyConverter(type, annotations, retrofit)
                HtmlMarker::class -> html.responseBodyConverter(type, annotations, retrofit)
                JsonMarker::class -> gson.responseBodyConverter(type, annotations, retrofit)
                else -> null
            }
        }
    }
}
