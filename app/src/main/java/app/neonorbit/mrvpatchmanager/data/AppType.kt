package app.neonorbit.mrvpatchmanager.data

import app.neonorbit.mrvpatchmanager.AppConfigs

enum class AppType {
    FACEBOOK,
    MESSENGER,
    FACEBOOK_LITE,
    MESSENGER_LITE,
    BUSINESS_SUITE;

    fun getName(): String {
        return AppConfigs.getFbAppName(this)
    }

    fun getPackage(): String {
        return AppConfigs.getFbAppPkg(this)
    }
}
