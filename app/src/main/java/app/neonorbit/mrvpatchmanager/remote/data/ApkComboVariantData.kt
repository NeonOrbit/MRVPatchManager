package app.neonorbit.mrvpatchmanager.remote.data

import pl.droidsonroids.jspoon.annotation.Selector

class ApkComboVariantData {
    @Selector("#variants-tab > div > ul > li")
    var variants: List<Variant> = listOf()

    @Selector("#best-variant-tab > div > ul > li")
    lateinit var fallback: Variant

    class Variant {
        @Selector("span")
        var arch: String = ""

        @Selector(".file-list > li")
        var apks: List<Apk> = listOf()
    }

    class Apk {
        @Selector(".vtype")
        var type: String = ""

        @Selector(".description")
        var info: String = ""

        @Selector("a.variant", attr = "href")
        var link: String = ""

        @Selector(".vername", defValue = "")
        private var _versionName: String? = null

        @Selector(".vercode", defValue = "")
        private var _versionCode: String? = null

        val versionName: String? by lazy {
            try {
                _versionName?.trim()?.substringAfterLast(' ')?.takeIf {
                    it.contains('.')
                }
            } catch (_: Exception) { null }
        }

        val versionCode: Long? by lazy {
            try {
                _versionCode?.trim('(',')')?.trim()?.toLong()
            } catch (_: Exception) { null }
        }
    }
}
