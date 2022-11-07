package app.neonorbit.mrvpatchmanager.ui.home

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neonorbit.mrvpatchmanager.AppConfig
import app.neonorbit.mrvpatchmanager.DefaultPreference
import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.download.DownloadStatus
import app.neonorbit.mrvpatchmanager.error
import app.neonorbit.mrvpatchmanager.event.ConfirmationEvent
import app.neonorbit.mrvpatchmanager.event.SingleEvent
import app.neonorbit.mrvpatchmanager.post
import app.neonorbit.mrvpatchmanager.postNow
import app.neonorbit.mrvpatchmanager.remote.GithubService
import app.neonorbit.mrvpatchmanager.repository.ApkRepository
import app.neonorbit.mrvpatchmanager.toMB
import app.neonorbit.mrvpatchmanager.toTempFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HomeViewModel : ViewModel() {
    private val repository = ApkRepository()

    val fbAppList = AppConfig.FB_APP_LIST
    var currentApp = AppConfig.FB_APP_LIST[0]

    val intentEvent = SingleEvent<Intent>()
    val messageEvent = SingleEvent<String>()
    val installEvent = SingleEvent<File>()
    val uninstallEvent = SingleEvent<Set<String>>()

    val confirmationEvent = ConfirmationEvent()

    var patchingJob = MutableStateFlow<Job?>(null)
    val progressStatus = MutableStateFlow<String?>(null)
    val progressTracker = MutableStateFlow(ProgressTrack())

    var quickDownloadJob = MutableStateFlow<Job?>(null)
    val quickDownloadProgress = MutableStateFlow<Int?>(null)

    private var moduleLatest: String? = null
    val moduleStatus: MutableStateFlow<VersionStatus> by lazy {
        MutableStateFlow(VersionStatus(null, null))
    }

    val warnFallback: Boolean get() = DefaultPreference.isFallbackMode()

    private val filePickerIntent: Intent by lazy {
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = ApkUtil.APK_MIME_TYPE
        }
    }

    private val catcher = CoroutineExceptionHandler { _, it ->
        val msg = "Failed: ${it.error}"
        viewModelScope.launch {
            quickDownloadProgress.emit(null)
            if (progressStatus.value != null) {
                progressStatus.emit(msg)
                progressTracker.emit(ProgressTrack(0))
            } else {
                messageEvent.post(msg)
            }
        }
    }

    fun reloadModuleStatus(force: Boolean = false) {
        if (force) GithubService.checkForUpdate()
        if (moduleStatus.value.current == null || force) {
            val version = ApkUtil.getPrefixedVersionName(AppConfig.MODULE_PACKAGE)
            if (version == null || version >= (moduleLatest ?: "")) {
                moduleLatest = null
            }
            moduleStatus.postNow(viewModelScope, VersionStatus(version, moduleLatest))
        }
    }

    fun updateModuleStatus(current: String, latest: String) {
        if (latest != moduleLatest) {
            moduleLatest = latest
            moduleStatus.postNow(viewModelScope, VersionStatus(current, moduleLatest))
        }
    }

    fun installModule(force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO + catcher) {
            install(repository.getModuleApk(force))
        }.let { job ->
            quickDownloadJob.post(viewModelScope, job)
        }
    }

    fun installManager() {
        viewModelScope.launch(Dispatchers.IO + catcher) {
            install(repository.getManagerApk())
        }.let { job ->
            quickDownloadJob.post(viewModelScope, job)
        }
    }

    fun manualRequest() {
        if (patchingJob.value == null) {
            intentEvent.post(viewModelScope, filePickerIntent)
        } else {
            viewModelScope.launch {
                messageEvent.post(
                    "A patching task is already in progress. Please cancel it first."
                )
            }
        }
    }

    fun patch(uri: Uri? = null) {
        var manual: File? = null
        viewModelScope.launch(Dispatchers.Default + catcher) {
            progressStatus.emit("Status")
            progressTracker.emit(ProgressTrack())
            val file = uri?.toTempFile()?.also {
                manual = it
                if (!ApkUtil.verifyFbSignature(it) &&
                    !confirmationEvent.ask("Warning", "This apk isn't official, continue?")) {
                    progressStatus.emit(null)
                    return@launch
                }
            } ?: withContext(Dispatchers.IO) {
                getPatchableApkFile(currentApp.type)
            }
            file?.takeIf {
                !ApkUtil.hasLatestMrvSignedApp(it) ||
                confirmationEvent.ask("Already on latest version, patch anyway?").also { r ->
                    if (!r) progressStatus.emit(null)
                }
            }?.let { apk ->
                progressTracker.emit(ProgressTrack())
                patchApk(apk)?.let { patched ->
                    progressTracker.emit(ProgressTrack(100, percent = ""))
                    install(patched)
                }
            }
        }.let { job ->
            patchingJob.post(viewModelScope, job)
            job.invokeOnCompletion {
                manual?.delete()
                if (it is CancellationException) {
                    viewModelScope.launch {
                        progressStatus.emit(null)
                        progressTracker.emit(ProgressTrack())
                    }
                } else if (progressTracker.value.current < 0) {
                    progressTracker.post(viewModelScope, ProgressTrack(0))
                }
                patchingJob.post(viewModelScope, null)
            }
        }
    }

    private suspend fun patchApk(input: File): File? {
        TODO("Not yet implemented")
    }

    private suspend fun getPatchableApkFile(type: AppType): File? {
        return repository.getFbApk(type).onEach { status ->
            when (status) {
                is DownloadStatus.FETCHING -> {
                    progressStatus.emit("Fetching: ${status.server}")
                }
                is DownloadStatus.DOWNLOADING -> {
                    progressStatus.emit("Downloading: ${type.getName()}")
                }
                is DownloadStatus.PROGRESS -> {
                    if (status.total < status.current) {
                        progressTracker.emit(ProgressTrack())
                    } else {
                        val percent = getPercentage(status.current, status.total)
                        val details = "${status.current.toMB()}/${status.total.toMB()}"
                        progressTracker.emit(ProgressTrack(percent, details))
                    }
                }
                is DownloadStatus.FINISHED -> {
                    progressTracker.emit(ProgressTrack(100))
                    progressStatus.emit("Downloaded: ${type.getName()}")
                }
                is DownloadStatus.FAILED -> {
                    throw Exception(status.error)
                }
            }
        }.catch {
            progressStatus.emit("Download Failed: ${it.message}")
            progressTracker.emit(ProgressTrack(0))
        }.lastOrNull()?.let {
            if (it is DownloadStatus.FINISHED) it.file else null
        }
    }

    private suspend fun install(download: Flow<DownloadStatus>) {
        quickDownloadProgress.emit(-1)
        download.onEach { status ->
            when (status) {
                is DownloadStatus.PROGRESS -> quickDownloadProgress.emit(
                    getPercentage(status.current, status.total)
                )
                is DownloadStatus.FAILED -> {
                    throw Exception(status.error)
                }
                else -> {}
            }
        }.catch {
            messageEvent.post("Failed: ${it.error}")
        }.onCompletion {
            quickDownloadProgress.emit(null)
        }.lastOrNull().let {
            if (it is DownloadStatus.FINISHED) {
                install(it.file)
            }
        }
    }

    private suspend fun install(file: File) {
        val conflicted = ApkUtil.getConflictedApps(file)
        if (conflicted.isNotEmpty()) {
            if (confirmationEvent.ask(
                    "Found apps with different signatures.\n" +
                        "Please uninstall these first:\n" +
                        "[${conflicted.values.joinToString(", ")}]"
                )) uninstallEvent.post(conflicted.keys)
        } else {
            if (confirmationEvent.ask("Install ${file.name}?")) {
                installEvent.post(file)
            }
        }
    }

    private fun getPercentage(current: Long, total: Long): Int {
        return if (total < current) -1 else (current * 100 / total).toInt()
    }

    data class VersionStatus(val current: String?, val latest: String? = null)

    class ProgressTrack(val current: Int = -1, val details: String = "∞", percent: String = "∞") {
        val percent = if (percent.isEmpty() || current < 0) "∞" else "$current%"
    }
}
