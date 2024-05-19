package app.neonorbit.mrvpatchmanager.repository

import app.neonorbit.mrvpatchmanager.data.AppType
import app.neonorbit.mrvpatchmanager.repository.data.ApkFileData
import app.neonorbit.mrvpatchmanager.download.DownloadStatus
import app.neonorbit.mrvpatchmanager.error
import app.neonorbit.mrvpatchmanager.local.ApkLocalFileProvider
import app.neonorbit.mrvpatchmanager.remote.ApkRemoteFileProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class ApkRepository {
    private val local = ApkLocalFileProvider()
    private val remote = ApkRemoteFileProvider()

    fun getFbApk(type: AppType, abi: String, version: String?) = remote.getFbApk(type, abi, version)

    fun getPatchedApks(): List<ApkFileData> = local.loadPatchedApks()

    fun getManagerApk(): Flow<DownloadStatus> = remote.getManagerApk()

    fun getModuleApk(force: Boolean = false): Flow<DownloadStatus> = flow {
        remote.getModuleApk().catch {
            tryLocalModule(force, it.error)
        }.collect {
            if (it is DownloadStatus.FAILED) {
                tryLocalModule(force, it.error)
            } else emit(it)
        }
    }

    private suspend fun FlowCollector<DownloadStatus>.tryLocalModule(force: Boolean, err: String) {
        try {
            if (!force) throw Exception()
            emit(DownloadStatus.FINISHED(local.getModuleApk()))
        } catch (_: Exception) {
            emit(DownloadStatus.FAILED(err))
        }
    }
}
