package app.neonorbit.mrvpatchmanager.network

object HttpSpec {
    object Header {
        const val E_TAG = "ETag"
        const val RANGE = "Range"
        const val IF_RANGE = "If-Range"
        const val USER_AGENT = "User-Agent"
        const val CONTENT_TYPE = "Content-Type"
        const val CACHE_CONTROL = "Cache-Control"
        const val CONTENT_RANGE = "Content-Range"
        const val IF_NONE_MATCH = "If-None-Match"
        const val LAST_MODIFIED = "Last-Modified"
        const val IF_MODIFIED_SINCE = "If-Modified-Since"
    }

    object Code {
        const val NOT_MODIFIED = 304
        const val PARTIAL_CONTENT = 206
        const val RANGE_NOT_SATISFIABLE = 416
    }
}
