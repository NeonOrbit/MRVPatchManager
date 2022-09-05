package app.neonorbit.mrvpatchmanager

import java.io.File

object AppServices {
    private val application: App get() = App.instance

    fun getCacheDir(): File = application.cacheDir
}
