package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.remote.ApkComboService
import app.neonorbit.mrvpatchmanager.util.Utils
import pl.droidsonroids.jspoon.annotation.Selector

class ApkComboVariantData {
    @Selector("#variants-tab > div > ul > li")
    var variants: List<Variant> = listOf()

    @Selector("#best-variant-tab > div > ul > li")
    lateinit var fallback: Variant

    override fun toString(): String {
        return "variants: $variants, fallback: $fallback"
    }

    class Variant {
        @Selector("span")
        var arch: String = ""

        @Selector(".file-list > li")
        var apks: List<Apk> = listOf()

        override fun toString(): String {
            return "arch: $arch, apks: $apks"
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    class Apk {
        @Selector(".vtype")
        var type: String = ""

        @Selector(".description")
        var info: String = ""

        @Selector("a.variant", attr = "href")
        private var _link: String = ""

        val link: String get() = Utils.absoluteUrl(
            ApkComboService.BASE_URL, _link
        )

        @Selector(".vername", defValue = "")
        private var _versionName: String? = null

        @Selector(".vercode", defValue = "")
        private var _versionCode: String? = null

        val versionName: String? get() = try {
            _versionName?.trim()?.substringAfterLast(' ')?.takeIf {
                it.contains('.')
            }
        } catch (_: Exception) { null }

        val versionCode: Long? get() = try {
            _versionCode?.trim('(',')')?.trim()?.toLong()
        } catch (_: Exception) { null }

        override fun toString(): String {
            return "type: $type, versionName: $versionName, versionCode: $versionCode, " +
                    "info: $info, link: $link"
        }
    }
}
