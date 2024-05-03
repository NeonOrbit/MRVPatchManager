package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.remote.ApkFlashService
import app.neonorbit.mrvpatchmanager.util.BypassedElementConverter
import app.neonorbit.mrvpatchmanager.util.Utils
import org.jsoup.nodes.Element
import pl.droidsonroids.jspoon.Jspoon
import pl.droidsonroids.jspoon.annotation.Selector

class ApkFlashVariantData {
    val variants: List<Variant> get() = _variants.ifEmpty { _fallback }

    @Selector("#variants", attr = "html", converter = VariantsExtractor::class)
    private var _variants: List<Variant> = listOf()

    @Selector("#download", attr = "html", converter = VariantsExtractor::class)
    private var _fallback: List<Variant> = listOf()

    override fun toString(): String {
        return "variants: $_variants, fallback: $_fallback"
    }

    data class Variant(val arch: String, val apks: List<Apk>) {
        override fun toString(): String {
            return "arch: $arch, apks: $apks"
        }
    }

    class Apk {
        @Selector(".vtype", defValue = "")
        private lateinit var type: String

        @Selector(".description", defValue = "")
        lateinit var info: String

        @Selector("a.variant", attr = "href")
        private lateinit var _link: String

        val link: String get() = Utils.absoluteUrl(
            ApkFlashService.BASE_URL, _link
        )

        @Selector(".vername")
        private var _versionName: String? = null

        val versionName: String? by lazy {
            _versionName?.let { ApkConfigs.extractVersionName(it) }
        }

        val isValidType: Boolean get() = type.trim().lowercase().let { it == "apk" || "xapk" !in it }

        override fun toString(): String {
            return "type: $type, versionName: $versionName, info: $info, link: $link"
        }
    }

    object VariantsExtractor : BypassedElementConverter<List<Variant>> {
        private val apkParser = Jspoon.create().adapter(Apk::class.java)

        override fun convert(node: Element?, selector: Selector): List<Variant> {
            return node?.select(".files-header:contains(arm)")?.map { arch ->
                Variant(
                    arch.text(),
                    arch.nextElementSibling().select(".variant").map {
                        apkParser.fromHtml(it.outerHtml())
                    }
                )
            } ?: listOf()
        }
    }
}
