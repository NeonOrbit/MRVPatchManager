package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.remote.ApkPureService
import app.neonorbit.mrvpatchmanager.util.NullableElementConverter
import app.neonorbit.mrvpatchmanager.util.Utils
import org.jsoup.nodes.Element
import pl.droidsonroids.jspoon.Jspoon
import pl.droidsonroids.jspoon.annotation.Selector

class ApkPureVariantData {
    val variants: List<Variant> get() = _variants.ifEmpty { _fallback }

    @Selector("main", attr = "html", converter = PrimaryExtractor::class)
    private var _fallback: List<Variant> = listOf()

    @Selector("#version-list", attr = "html", converter = VariantsExtractor::class)
    private var _variants: List<Variant> = listOf()

    override fun toString(): String {
        return "variants: $_variants, fallback: $_fallback"
    }

    data class Variant(val arch: String, val apks: List<Apk>) {
        override fun toString(): String {
            return "arch: $arch, apks: $apks"
        }
    }

    class Apk {
        @Selector(".name", defValue = "")
        private lateinit var name: String

        @Selector(".tag:contains(apk)", defValue = "")
        private lateinit var type: String

        @Selector(".sdk:contains(android)", defValue = "")
        private lateinit var sdk: String

        @Selector("a.download-btn", attr = "href")
        private lateinit var href: String

        val minSDk: Int? get() = ApkConfigs.extractMinSdk(sdk)

        val version: String? get() = ApkConfigs.extractVersionName(name)

        val link: String get() = Utils.absoluteUrl(ApkPureService.BASE_URL, href)

        val isValidType: Boolean get() = true
        //val isValidType: Boolean get() = type.trim().lowercase().let { it == "apk" || "xapk" !in it }

        override fun toString(): String {
            return "type: $type, version: $version, minSDk: $minSDk, link: $link"
        }

        companion object {
            fun build(name: String, type: String, sdk: String, href: String) = Apk().apply {
                this.name = name; this.type = type; this.sdk = sdk; this.href = href
            }
        }
    }

    object VariantsExtractor : NullableElementConverter<List<Variant>> {
        private val apkParser = Jspoon.create().adapter(Apk::class.java)

        override fun convert(node: Element?, selector: Selector): List<Variant> {
            return node?.select(".group-title:matches(\\barm(?:eabi|64)-(?:v7a|v8a)\\b)")?.map { arch ->
                val items = mutableListOf<Apk>()
                var current = arch.nextElementSibling()
                while (current?.hasClass("apk") == true) {
                    items.add(apkParser.fromHtml(current.select(".apk").html()))
                    current = current.nextElementSibling()
                }
                Variant(arch.text(), items)
            } ?: listOf()
        }
    }

    object PrimaryExtractor : NullableElementConverter<List<Variant>> {
        override fun convert(node: Element?, selector: Selector): List<Variant> = node?.let { main ->
            val name = main.select(".info-content .info-sdk").firstOrNull()?.text()
            val type = main.select(".info-content .info-tag").firstOrNull()?.text()
            val arch = main.select(".more-info .info:contains(Architecture)").select(".value").firstOrNull()?.text()
            val sdk = main.select(".more-info .info:contains(Requires Android)").select(".value").firstOrNull()?.text()
            val href = main.select("a#download_link").ifEmpty { main.select("a.download-start-btn") }.firstOrNull()?.attr("href")
            if (name != null && type != null && arch != null && href != null) {
                listOf(Variant(arch, listOf(Apk.build(name, type, sdk ?: "", href))))
            } else null
        } ?: listOf()
    }
}
