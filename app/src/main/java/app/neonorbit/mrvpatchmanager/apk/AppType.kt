package app.neonorbit.mrvpatchmanager.apk

import app.neonorbit.mrvpatchmanager.AppConfig

enum class AppType {
    FACEBOOK,
    MESSENGER,
    FACEBOOK_LITE,
    MESSENGER_LITE,
    BUSINESS_SUITE;

    fun getName(): String {
        return AppConfig.getFbAppName(this)
    }
}
