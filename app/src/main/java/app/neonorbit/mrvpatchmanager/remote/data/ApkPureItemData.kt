package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.remote.ApkPureService
import app.neonorbit.mrvpatchmanager.util.BypassedElementConverter
import app.neonorbit.mrvpatchmanager.util.Utils
import org.jsoup.nodes.Element
import pl.droidsonroids.jspoon.annotation.Selector

class ApkPureItemData {
    @Selector("body")
    var item: Item? = null

    @Selector("#version-list", attr = "html", converter = VariantsExtractor::class)
    var variants: List<Item> = listOf()

    override fun toString(): String = "item: $item, variants: $variants"

    class Item {
        @Selector("a.info-tag", defValue = "")
        private lateinit var type: String

        @Selector("div:contains(Architecture) + div", defValue = "")
        lateinit var arch: String

        @Selector("div:contains(Requires Android) + div", defValue = "")
        lateinit var min: String

        @Selector("#download_link", attr = "href")
        private lateinit var _link: String

        val link: String get() = Utils.absoluteUrl(ApkPureService.BASE_URL, _link)

        @Selector(".info-sdk")
        private var _version: String? = null

        val version: String? by lazy {
            _version?.let { ApkConfigs.extractVersionName(it) }
        }

        val isValidType: Boolean get() = type.trim().lowercase().let { it == "apk" }

        override fun toString(): String = "type: $type, arch: $arch, min: $min, version: $version, link: $link"

        companion object {
            fun build(type: String, arch: String, min: String, version: String, link: String) = Item().apply {
                this.type = type; this.arch = arch; this.min = min; this._version = version; this._link = link
            }
        }
    }

    object VariantsExtractor : BypassedElementConverter<List<Item>> {
        override fun convert(node: Element?, selector: Selector): List<Item> {
            val items = mutableListOf<Item>()
            node?.select(".group-title:contains(arm)")?.take(5)?.forEach { arch ->
                try {
                    extractApks(arch.text(), arch, items)
                } catch (_: Exception) {}
            }
            return items
        }
        private fun extractApks(arch: String, section: Element, items: MutableList<Item>) {
            var current = section.nextElementSibling()
            repeat(10) {
                if (current?.hasClass("apk") != true) return
                val type = current.select(".tag:contains(apk)").firstOrNull()?.text()
                val min = current.select(".sdk:contains(android)").firstOrNull()?.text()
                val link = current.select("a.download-btn").firstOrNull()?.attr("href")
                val version = current.select(".name:matches(.*.\\d+(?:\\.\\d+){3,}.*)").firstOrNull()?.text() ?: ""
                if (type != null && min != null && link != null) {
                    items.add(Item.build(type, arch, min, version, link))
                }
                current = current.nextElementSibling()
            }
        }
    }
}
