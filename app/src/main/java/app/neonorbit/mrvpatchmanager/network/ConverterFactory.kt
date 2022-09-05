package app.neonorbit.mrvpatchmanager.network

import app.neonorbit.mrvpatchmanager.network.parser.HtmlParser
import app.neonorbit.mrvpatchmanager.network.parser.JsonParser
import app.neonorbit.mrvpatchmanager.network.parser.XmlParser
import okhttp3.ResponseBody
import pl.droidsonroids.retrofit2.JspoonConverterFactory
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.lang.reflect.Type

class ConverterFactory : Converter.Factory() {
    companion object {
        val PARSERS = listOf(
            XmlParser::class, HtmlParser::class, JsonParser::class
        )
    }

    private val gson: GsonConverterFactory by lazy {
        GsonConverterFactory.create()
    }

    private val html: JspoonConverterFactory by lazy {
        JspoonConverterFactory.create()
    }

    private val xml: SimpleXmlConverterFactory by lazy {
        SimpleXmlConverterFactory.create()
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
                XmlParser::class -> xml.responseBodyConverter(type, annotations, retrofit)
                HtmlParser::class -> html.responseBodyConverter(type, annotations, retrofit)
                JsonParser::class -> gson.responseBodyConverter(type, annotations, retrofit)
                else -> null
            }
        }
    }
}
