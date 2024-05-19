package app.neonorbit.mrvpatchmanager

import android.app.Application
import app.neonorbit.mrvpatchmanager.remote.GithubService

class MRVPatchManager : Application() {
    companion object {
        lateinit var instance: MRVPatchManager
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        onAppInitialized()
    }

    private fun onAppInitialized() {
        AppInstaller.register(this)
        GithubService.checkForUpdate()
    }
}
