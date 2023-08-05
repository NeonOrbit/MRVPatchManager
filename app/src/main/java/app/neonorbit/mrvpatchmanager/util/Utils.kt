package app.neonorbit.mrvpatchmanager.util

import android.util.Log
import app.neonorbit.mrvpatchmanager.AppConfig
import app.neonorbit.mrvpatchmanager.BuildConfig

@Suppress("FunctionName", "MemberVisibilityCanBePrivate", "Unused")
object Utils {
    fun log(msg: String) = Log.d(AppConfig.APP_TAG, msg)
    fun warn(msg: String) = Log.w(AppConfig.APP_TAG, msg)
    fun error(msg: String) = Log.e(AppConfig.APP_TAG, msg)
    fun warn(msg: String, t: Throwable) = Log.w(AppConfig.APP_TAG, msg, t)
    fun error(msg: String, t: Throwable) = Log.e(AppConfig.APP_TAG, msg, t)

    fun <T> T.LOG(msg: String): T {
        if (BuildConfig.DEBUG) log("$msg: $this")
        return this
    }

    fun absoluteUrl(host: String, url: String): String {
        return when {
            url.isEmpty() || url.startsWith("http") -> url
            url[0] == '/' -> "$host$url"
            else -> "$host/$url"
        }
    }
}
