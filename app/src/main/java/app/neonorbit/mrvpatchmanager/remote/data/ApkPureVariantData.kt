package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.remote.ApkPureService
import app.neonorbit.mrvpatchmanager.util.Utils
import org.jsoup.nodes.Element
import pl.droidsonroids.jspoon.ElementConverter
import pl.droidsonroids.jspoon.annotation.Selector

class ApkPureVariantData {
    @Selector(".ver-info-m")
    var variants: List<Variant> = listOf()

    override fun toString(): String {
        return "variants: $variants"
    }

    class Variant {
        @Selector("p:contains(Screen DPI)", converter = OwnText::class)
        var dpi: String = ""

        @Selector("p:contains(Architecture)", converter = OwnText::class)
        var arch: String = ""

        @Selector("p:contains(Requires Android)", converter = OwnText::class)
        var min: String = ""

        @Selector("a.down", attr = "href")
        private lateinit var _link: String

        val link: String get() = Utils.absoluteUrl(
            ApkPureService.BASE_URL, _link
        )

        override fun toString(): String {
            return "dpi: $dpi, arch: $arch, min: $min, link: $link"
        }
    }

    object OwnText : ElementConverter<String> {
        override fun convert(node: Element, selector: Selector): String {
            return node.ownText()
        }
    }
}
