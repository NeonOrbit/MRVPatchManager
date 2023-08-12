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

    fun sdkToVersion(sdk: Int) = when (
        if (sdk < 14) 0 else if (sdk in 14..20) 20 else sdk
    ) {
        0 -> 0
        20 -> 4
        21, 22 -> 5
        23 -> 6
        24, 25 -> 7
        26, 27 -> 8
        28 -> 9
        29 -> 10
        30 -> 11
        31, 32 -> 12
        else -> sdk - 20
    }
}
