package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.remote.ApkComboService
import app.neonorbit.mrvpatchmanager.util.Utils
import pl.droidsonroids.jspoon.annotation.Selector

class ApkComboVariantData {
    @Selector("#variants-tab ul:not(ul.file-list) > li")
    var variants: List<Variant> = listOf()

    @Selector("#best-variant-tab .file-list li")
    var fallback: Apk? = null

    override fun toString(): String {
        return "variants: $variants, fallback: $fallback"
    }

    class Variant {
        @Selector("span:contains(arm), code:contains(arm)", defValue = "")
        lateinit var arch: String

        @Selector(".file-list li")
        var apks: List<Apk> = listOf()

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
            ApkComboService.BASE_URL, _link
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
}
