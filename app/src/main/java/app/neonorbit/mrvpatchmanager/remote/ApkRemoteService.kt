package app.neonorbit.mrvpatchmanager.remote

import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.isConnectError
import app.neonorbit.mrvpatchmanager.remote.data.RemoteApkInfo
import app.neonorbit.mrvpatchmanager.util.Utils
import kotlin.coroutines.cancellation.CancellationException

interface ApkRemoteService {
    fun server(): String
    suspend fun fetch(type: AppType, abi: String, ver: String?): RemoteApkInfo

    fun Exception.handleApkServiceException(type: AppType, ver: String?): Nothing {
        handleUnrelated()
        throwServiceException(type, ver)
    }

    fun Exception.handleApkServiceException(type: AppType, ver: String?, strict: Boolean) {
        this.handleUnrelated()
        if (strict) throwServiceException(type, ver)
    }

    private fun Exception.handleUnrelated() {
        if (this is CancellationException || this.isConnectError) throw this
    }

    private fun Exception.throwServiceException(type: AppType, ver: String?): Nothing {
        this.message?.let { Utils.warn(it, this) }
        throw Exception(
            ver?.let { "${type.getName()} version '$it' is not available"} ?:
            "Couldn't fetch apk info from the server"
        )
    }
}
