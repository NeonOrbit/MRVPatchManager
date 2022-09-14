package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.apk.AppType

interface ApkRemoteService {
    fun server(): String
    suspend fun fetchLink(type: AppType): String
}
