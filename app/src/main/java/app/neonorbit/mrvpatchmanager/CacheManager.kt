package app.neonorbit.mrvpatchmanager

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit.HOURS

object CacheManager {
    fun put(key: String, value: Any, hours: Int) {
        DefaultPreference.putCache(key, serialize(value, hours.toLong()))
    }

    inline fun <reified T> get(key: String, force: Boolean = false): T? {
        return DefaultPreference.getCache(key)?.let {
            try {
                val token = object : TypeToken<CachedData<T>>() {}
                (Gson().fromJson(it, token.type) as CachedData<T>?)?.get(force)
            } catch (_: Exception) { null }
        }
    }

    private fun serialize(value: Any, hours: Long): String {
        return Gson().toJson(CachedData(value, hours))
    }

    class CachedData<T>(private val data: T, private val hours: Long) {
        private val created = System.currentTimeMillis()

        fun get(force: Boolean = false): T? = if (isValid() || force) data else null

        private fun isValid(): Boolean {
            return (System.currentTimeMillis() - created) < HOURS.toMillis(hours)
        }
    }
}
