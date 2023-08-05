package app.neonorbit.mrvpatchmanager.remote.data

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "rss", strict = false)
data class RssFeedData (
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
        )
    }
}
