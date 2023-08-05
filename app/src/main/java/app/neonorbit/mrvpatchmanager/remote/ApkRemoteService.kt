package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.remote.data.RemoteApkInfo

interface ApkRemoteService {
    fun server(): String
    suspend fun fetch(type: AppType, abi: String): RemoteApkInfo
}
