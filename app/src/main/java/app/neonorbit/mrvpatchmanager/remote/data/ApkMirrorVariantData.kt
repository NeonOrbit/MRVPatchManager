package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.remote.ApkMirrorService
import app.neonorbit.mrvpatchmanager.util.Utils
import pl.droidsonroids.jspoon.annotation.Selector

class ApkMirrorVariantData {
    @Selector(".variants-table .table-row:has(a):contains(arm):contains(dpi)")
    var variants: List<Variant> = listOf()

    override fun toString(): String {
        return "variants: $variants"
    }

    class Variant {
        @Selector(".table-cell:contains(arm)", defValue = "")
        lateinit var arch: String

        @Selector(".table-cell:contains(dpi)", defValue = "")
        lateinit var dpi: String

        @Selector(".table-cell:matches(\\bAndroid\\s*\\W+(\\d+)(?:\\.\\d+)*\\+)", defValue = "")
        lateinit var min: String

        @Selector("a[href*=download]", attr = "href", defValue = "")
        private lateinit var _link: String

        val link: String get() = Utils.absoluteUrl(ApkMirrorService.BASE_URL, _link)

        override fun toString(): String {
            return "arch: $arch, dpi: $dpi, min: $min, link: $link"
        }
    }
}
