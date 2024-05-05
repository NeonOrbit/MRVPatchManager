package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.remote.ApkMirrorService
import app.neonorbit.mrvpatchmanager.util.Utils
import pl.droidsonroids.jspoon.annotation.Selector

class ApkMirrorVariantData {
    @Selector(".variants-table .table-row:has(a):matches(\\barm(?:eabi|64)-(?:v7a|v8a)\\b):contains(dpi)")
    var variants: List<Variant> = listOf()

    override fun toString(): String {
        return "variants: $variants"
    }

    class Variant {
        @Selector(value = ".apkm-badge", defValue = "")
        private lateinit var type: String

        @Selector("a[href*=download]:matches(\\b(?<!\\.)(\\d+(?:\\.\\d+){3,5})(?!\\.)\\b)", defValue = "")
        private lateinit var versionText: String

        @Selector(".table-cell:matches(\\barm(?:eabi|64)-(?:v7a|v8a)\\b)", defValue = "")
        lateinit var arch: String

        @Selector(".table-cell:matches(\\b(?:nodpi|\\d+dpi)\\b)", defValue = "")
        private lateinit var dpiRaw: String

        @Selector(".table-cell:matches(\\bAndroid\\s*\\W+(\\d+)(?:\\.\\d+)*\\+)", defValue = "")
        private lateinit var sdkRaw: String

        @Selector("a[href*=download]", attr = "href", defValue = "")
        private lateinit var href: String

        val dpi: String? get() = dpiRaw.takeIf { "dpi" in it }

        val minSDk: Int? get() = ApkConfigs.extractMinSdk(sdkRaw)

        val version: String? get() = ApkConfigs.extractVersionName(versionText)

        val link: String get() = Utils.absoluteUrl(ApkMirrorService.BASE_URL, href)

        val isValidType: Boolean get() = type.trim().lowercase().let {
            it == "apk" || ("xapk" !in it && "bundle" !in it )
        }

        override fun toString(): String {
            return "arch: $arch, type: $type, dpi: $dpi, minSDk: $minSDk, version: $version, link: $link"
        }
    }
}
