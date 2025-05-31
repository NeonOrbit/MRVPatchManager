package app.neonorbit.mrvpatchmanager.remote

import android.util.Log
import app.neonorbit.mrvpatchmanager.AppConfigs
import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.DefaultPreference
import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import app.neonorbit.mrvpatchmanager.data.AppType
import app.neonorbit.mrvpatchmanager.download.ApkDownloader
import app.neonorbit.mrvpatchmanager.download.DownloadStatus
import app.neonorbit.mrvpatchmanager.toNetworkError
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import java.io.File
import java.security.SignatureException

class ApkRemoteFileProvider {
    companion object {
        val services = listOf(
            ApkMirrorService,
            ApkComboService,
            ApkPureService,
            // ApkFlashService    // Unavailable
        )
        private const val CACHED_THRESHOLD = 20L * 60 * 60 * 1000
        private val TAG = ApkRemoteFileProvider::class.simpleName
    }

    private fun getServices(): Iterator<ApkRemoteService> {
        return DefaultPreference.getApkServer()?.let { server ->
            services.firstOrNull { server == it.server() }?.let {
                listOf(it).iterator()
            }
        } ?: services.iterator()
    }

    fun getManagerApk(): Flow<DownloadStatus> = flow {
        ApkDownloader.download(
            GithubService.getManagerLink(),
            File(AppConfigs.DOWNLOAD_DIR, AppConfigs.MANAGER_APK_NAME)
        ).catch {
            emit(DownloadStatus.FAILED(it.toNetworkError()))
        }.let { emitAll(it) }
    }

    fun getModuleApk(): Flow<DownloadStatus> = flow {
        ApkDownloader.download(
            GithubService.getModuleLink(),
            File(AppConfigs.DOWNLOAD_DIR, AppConfigs.MODULE_APK_NAME)
        ).catch {
            emit(DownloadStatus.FAILED(it.toNetworkError()))
        }.let { emitAll(it) }
    }

    fun getFbApk(type: AppType, abi: String, version: String?): Flow<DownloadStatus> {
        val file = AppConfigs.getDownloadApkFile(type, version)
        if (hasValidFile(file, version)) {
            return flowOf(DownloadStatus.FINISHED(file))
        }
        val services = getServices()
        var service: ApkRemoteService = services.next()
        return flow {
            emit(DownloadStatus.FETCHING(service.server()))
            val fetched = service.fetch(type, abi, version)
            fetched.version?.let {
                emit(DownloadStatus.FETCHED(it))
            }
            ApkDownloader.download(fetched.link, file).onEach {
                if (it is DownloadStatus.FINISHED) {
                    if (!ApkUtil.verifyFbSignature(it.file)) {
                        it.file.delete()
                        throw SignatureException("Signature failed")
                    }
                }
            }.let { emitAll(it) }
        }.retryWhen { e, _ ->
            Log.w(TAG, "getFbApk[${service.server()}]", e)
            AppServices.isNetworkOnline() && services.hasNext().also {
                if (it) {
                    service = services.next()
                    val err = if (e is SignatureException) e.message else "Failed"
                    emit(DownloadStatus.FETCHING("$err, trying the next server..."))
                    delay(3000)
                }
            }
        }.catch { exception ->
            val isOnline = AppServices.isNetworkOnline()
            emit(DownloadStatus.FAILED(exception.toNetworkError(isOnline)))
        }
    }

    private fun hasValidFile(file: File, version: String?): Boolean {
        val last = file.lastModified()
        val current = System.currentTimeMillis()
        return file.exists() && (current - last < CACHED_THRESHOLD) && try {
            ApkUtil.verifyFbSignatureWithVersion(file, version)
        } catch (_: Exception) { false }
    }
}
