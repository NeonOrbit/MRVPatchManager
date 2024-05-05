package app.neonorbit.mrvpatchmanager.remote.data

import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "rss", strict = false)
data class ApkMirrorRssFeedData (
    @field:Element(name = "channel")
    @param:Element(name = "channel")
    val channel: RssChannel
) {
    @Root(name = "channel", strict = false)
    data class RssChannel (
        @field:ElementList(name = "item", inline = true)
        @param:ElementList(name = "item", inline = true)
        val items: List<RssItem>
    ) {
        @Root(name = "item", strict = false)
        data class RssItem (
            @field:Element(name = "title")
            @param:Element(name = "title")
            val title: String,

            @field:Element(name = "link")
            @param:Element(name = "link")
            val link: String
        ) {
            val dpi: String? get() = title.takeIf { "dpi" in it }
            val minSDk: Int? get() = ApkConfigs.extractMinSdk(title)
            val version: String? get() = ApkConfigs.extractVersionName(title)
        }
    }
}
