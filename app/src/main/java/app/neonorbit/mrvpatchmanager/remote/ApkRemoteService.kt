package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.isConnectError
import app.neonorbit.mrvpatchmanager.remote.data.RemoteApkInfo
import app.neonorbit.mrvpatchmanager.util.Utils
import kotlin.coroutines.cancellation.CancellationException

interface ApkRemoteService {
    fun server(): String
    suspend fun fetch(type: AppType, abi: String, ver: String?): RemoteApkInfo

    fun Exception.handleApkServiceException(ver: String?): Nothing {
        handleUnrelated()
        throwServiceException(ver)
    }

    fun Exception.handleApkServiceException(ver: String?, skip: Boolean) {
        this.handleUnrelated()
        if (ver != null || !skip) throwServiceException(ver)
    }

    private fun Exception.handleUnrelated() {
        if (this is CancellationException || this.isConnectError) throw this
    }

    private fun Exception.throwServiceException(ver: String?): Nothing {
        this.message?.let { Utils.warn(it, this) }
        throw Exception(
            ver?.let { "$it version not found"} ?:
            "Couldn't fetch apk info from the server"
        )
    }
}
