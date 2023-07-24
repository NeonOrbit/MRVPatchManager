package app.neonorbit.mrvpatchmanager.util

object Utils {
    fun absoluteUrl(host: String, url: String): String {
        return when {
            url.isEmpty() || url.startsWith("http") -> url
            url[0] == '/' -> "$host$url"
            else -> "$host/$url"
        }
    }
}
