package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.remote.ApkComboService
import app.neonorbit.mrvpatchmanager.util.Utils
import pl.droidsonroids.jspoon.annotation.Selector

class ApkComboVariantData {
    val variants: List<Variant> get() = _variants.ifEmpty { _fallback }

    @Selector("#variants-tab ul:not(ul.file-list) > li")
    private var _variants: List<Variant> = listOf()

    @Selector("#best-variant-tab ul:not(ul.file-list) > li")
    private var _fallback: List<Variant> = listOf()

    override fun toString(): String {
        return "variants: $_variants, fallback: $_fallback"
    }

    class Variant {
        @Selector("span:matches(\\barm(?:eabi|64)-(?:v7a|v8a)\\b), code:contains(arm)", defValue = "")
        lateinit var arch: String

        @Selector(".file-list > li")
        var apks: List<Apk> = listOf()

        override fun toString(): String {
            return "arch: $arch, apks: $apks"
        }
    }

    class Apk {
        @Selector(".vtype", defValue = "")
        private lateinit var type: String

        @Selector(".vername", defValue = "")
        private lateinit var name: String

        @Selector(".description", defValue = "")
        private lateinit var info: String

        @Selector("a.variant", attr = "href")
        private lateinit var href: String

        val dpi: String? get() = info.takeIf { "dpi" in it }

        val minSDk: Int? get() = ApkConfigs.extractMinSdk(info)

        val version: String? get() = ApkConfigs.extractVersionName(name)

        val link: String get() = Utils.absoluteUrl(ApkComboService.BASE_URL, href)

        val isValidType: Boolean get() = true
        //val isValidType: Boolean get() = type.trim().lowercase().let { it == "apk" || "xapk" !in it }

        override fun toString(): String {
            return "type: $type, version: $version, dpi: $dpi, minSDk: $minSDk, link: $link"
        }
    }
}
