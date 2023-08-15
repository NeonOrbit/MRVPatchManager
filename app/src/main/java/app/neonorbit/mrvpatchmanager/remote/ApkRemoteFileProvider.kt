package app.neonorbit.mrvpatchmanager.remote

import android.util.Log
import app.neonorbit.mrvpatchmanager.AppConfig
import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.DefaultPreference
import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.download.DownloadStatus
import app.neonorbit.mrvpatchmanager.download.FileDownloader
import app.neonorbit.mrvpatchmanager.error
import app.neonorbit.mrvpatchmanager.toNetworkError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import java.io.File

class ApkRemoteFileProvider {
    companion object {
        val services = listOf(ApkMirrorService, ApkComboService, ApkPureService)
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
        FileDownloader.download(
            GithubService.getManagerLink(),
            File(AppConfig.DOWNLOAD_DIR, AppConfig.MANAGER_APK_NAME)
        ).catch {
            emit(DownloadStatus.FAILED(it.error))
        }.let { emitAll(it) }
    }

    fun getModuleApk(): Flow<DownloadStatus> = flow {
        FileDownloader.download(
            GithubService.getModuleLink(),
            File(AppConfig.DOWNLOAD_DIR, AppConfig.MODULE_APK_NAME)
        ).catch {
            emit(DownloadStatus.FAILED(it.error))
        }.let { emitAll(it) }
    }

    fun getFbApk(type: AppType, abi: String, version: String?): Flow<DownloadStatus> {
        val file = AppConfig.getDownloadApkFile(type, version)
        if (hasValidFile(file, version)) {
            return flowOf(DownloadStatus.FINISHED(file))
        } else if (version != null) file.delete()
        val iterator = getServices()
        var service: ApkRemoteService = iterator.next()
        return flow {
            emit(DownloadStatus.FETCHING(service.server()))
            val fetched = service.fetch(type, abi, version)
            fetched.version?.let {
                emit(DownloadStatus.FETCHED(it))
            }
            FileDownloader.download(fetched.link, file).onEach {
                if (it is DownloadStatus.FINISHED) {
                    if (!ApkUtil.verifyFbSignature(it.file)) {
                        it.file.delete()
                        throw Exception("Signature verification failed")
                    }
                }
            }.let { emitAll(it) }
        }.retryWhen { exception, _ ->
            Log.w(TAG, "getFbApk[${service.server()}]", exception)
            AppServices.isNetworkOnline() && iterator.hasNext().also {
                if (it) service = iterator.next()
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
