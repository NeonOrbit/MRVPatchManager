package app.neonorbit.mrvpatchmanager.ui.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neonorbit.mrvpatchmanager.AppConfig
import app.neonorbit.mrvpatchmanager.DefaultPatcher
import app.neonorbit.mrvpatchmanager.DefaultPatcher.PatchStatus
import app.neonorbit.mrvpatchmanager.DefaultPreference
import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import app.neonorbit.mrvpatchmanager.apk.AppType
import app.neonorbit.mrvpatchmanager.compareVersion
import app.neonorbit.mrvpatchmanager.download.DownloadStatus
import app.neonorbit.mrvpatchmanager.error
import app.neonorbit.mrvpatchmanager.event.ConfirmationEvent
import app.neonorbit.mrvpatchmanager.event.SingleEvent
import app.neonorbit.mrvpatchmanager.post
import app.neonorbit.mrvpatchmanager.postNow
import app.neonorbit.mrvpatchmanager.remote.GithubService
import app.neonorbit.mrvpatchmanager.repository.ApkRepository
import app.neonorbit.mrvpatchmanager.toSize
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
import java.io.File
import kotlin.coroutines.coroutineContext

class HomeViewModel : ViewModel() {
    private val repository = ApkRepository()

    val fbAppList = AppConfig.FB_APP_LIST

    val uriEvent = SingleEvent<Uri>()
    val intentEvent = SingleEvent<Intent>()
    val messageEvent = SingleEvent<String>()
    val installEvent = SingleEvent<File>()
    val uninstallEvent = SingleEvent<Set<String>>()

    val confirmationEvent = ConfirmationEvent()

    val patchingStatus = MutableStateFlow(false)
    val progressStatus = MutableStateFlow<String?>(null)
    val progressTracker = MutableStateFlow(ProgressTrack())

    val quickDownloadJob = MutableStateFlow<Job?>(null)
    val quickDownloadProgress = MutableStateFlow<Int?>(null)

    private var moduleLatest: String? = null
    val moduleStatus: MutableStateFlow<VersionStatus> by lazy {
        MutableStateFlow(VersionStatus(null, null))
    }

    val isFallback: Boolean get() = DefaultPreference.isFallbackEnabled()
    val fixConflict: Boolean get() = DefaultPreference.isFixConflictEnabled()
    val maskPackage: Boolean get() = DefaultPreference.isPackageMaskEnabled()

    private var patchingJob: Job? = null

    private val moduleInfoIntent: Intent by lazy {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", AppConfig.MODULE_PACKAGE, null)
        )
    }

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
            if ((version?.compareVersion(moduleLatest) ?: 0) >= 0) {
                moduleLatest = null
            }
            moduleStatus.postNow(VersionStatus(version, moduleLatest), with = this)
        }
    }

    fun updateModuleStatus(current: String, latest: String) {
        if (latest != moduleLatest) {
            moduleLatest = latest
            moduleStatus.postNow(VersionStatus(current, moduleLatest), with = this)
        }
    }

    fun showModuleInfo() = intentEvent.post(viewModelScope, moduleInfoIntent)
    fun visitModule() = uriEvent.post(viewModelScope, Uri.parse(AppConfig.MODULE_LATEST_URL))
    fun visitManager() = uriEvent.post(viewModelScope, Uri.parse(AppConfig.MANAGER_LATEST_URL))

    fun installModule(force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO + catcher) {
            install(repository.getModuleApk(force))
        }.let { job ->
            quickDownloadJob.post(job, with = this)
        }
    }

    fun installManager() {
        viewModelScope.launch(Dispatchers.IO + catcher) {
            install(repository.getManagerApk())
        }.let { job ->
            quickDownloadJob.post(job, with = this)
        }
    }

    fun manualRequest() {
        if (patchingJob == null) {
            intentEvent.post(viewModelScope, filePickerIntent)
        } else {
            viewModelScope.launch {
                messageEvent.post(
                    "A patching task is already in progress. Please cancel it first."
                )
            }
        }
    }

    fun patch(app: AppType? = null, uri: Uri? = null) {
        if (patchingJob != null) {
            progressStatus.post("Stopping...", this)
            patchingJob?.cancel()
            return
        }
        var manual: File? = null
        viewModelScope.launch(Dispatchers.Default + catcher) {
            progressStatus.emit("Progress...")
            progressTracker.emit(ProgressTrack())
            val file = uri?.toTempFile()?.also {
                manual = it
            } ?: app?.takeIf { checkMaskPackage(it) }?.let {
                getPatchableApkFile(app)
            }
            if (file != null && checkPreconditions(file, manual != null)) {
                progressTracker.emit(ProgressTrack())
                patchApk(file)?.let { patched ->
                    progressTracker.emit(ProgressTrack(100, percent = ""))
                    install(patched)
                }
            } else {
                progressStatus.emit(null)
            }
        }.let { job ->
            patchingJob = job
            patchingStatus.post(true, with = this)
            job.invokeOnCompletion {
                manual?.delete()
                if (it is CancellationException) {
                    viewModelScope.launch {
                        progressStatus.emit(null)
                        progressTracker.emit(ProgressTrack())
                    }
                } else if (progressTracker.value.current < 0) {
                    progressTracker.post(ProgressTrack(0), with = this)
                }
                patchingJob = null
                patchingStatus.post(false, with = this)
            }
        }
    }

    private suspend fun checkMaskPackage(app: AppType? = null, file: File? = null): Boolean {
        if (maskPackage && !(ApkUtil.isMessenger(app) || ApkUtil.isMessenger(file))) {
            messageEvent.post("[Mask package name] option is currently enabled, " +
                    "only Messenger is allowed to be patched in this mode."
            )
            return false
        }
        return true
    }

    private suspend fun checkPreconditions(file: File, isManual: Boolean): Boolean {
        if (!checkMaskPackage(file = file)) return false
        val passed = !fixConflict || ApkUtil.isMessenger(file) || confirmationEvent.ask(
            "[Resolve apk conflicts] option is currently enabled, there's no need to patch any other apps besides Messenger. Patch anyway?"
        )
        if (!isManual || !passed) return passed
        return ApkUtil.verifyFbSignature(file, false) || confirmationEvent.ask("Warning!", "This apk isn't official, continue?") &&
                (!ApkUtil.hasLatestMrvSignedApp(file) || confirmationEvent.ask("Already on the latest version, patch anyway?"))
    }

    private suspend fun patchApk(input: File): File? {
        val patched = AppConfig.getPatchedApkFile(input) ?: throw Exception(
            "Failed to get patched apk info"
        )
        if (patched.exists() && !confirmationEvent.ask(
                "${patched.name} already exists in the patched apk list, patch again?"
            )) {
            progressStatus.emit(null)
            return null
        }
        patched.delete()
        progressStatus.emit("Patching...")
        val opt = DefaultPatcher.Options(isFallback, fixConflict, maskPackage)
        return DefaultPatcher.patch(input, opt).onEach { status ->
            when (status) {
                is PatchStatus.PATCHING -> progressStatus.emit("Patching: ${status.msg}")
                is PatchStatus.FINISHED -> {
                    status.file.copyTo(patched, true)
                    status.file.delete()
                    progressStatus.emit("Patched: ${patched.name}")
                }
                is PatchStatus.FAILED -> {
                    throw Exception(status.msg)
                }
            }
        }.catch {
            progressStatus.emit("Patch Failed: ${it.error}")
            progressTracker.emit(ProgressTrack(0))
        }.lastOrNull().let {
            if (it is PatchStatus.FINISHED) patched else null
        }
    }

    private suspend fun getPatchableApkFile(type: AppType): File? {
        var version = "latest"
        return repository.getFbApk(type).onEach { status ->
            when (status) {
                is DownloadStatus.FETCHING -> {
                    progressStatus.emit("Fetching: ${status.server}")
                }
                is DownloadStatus.FETCHED -> {
                    version = status.version
                    if (ApkUtil.hasLatestMrvSignedApp(type.getPackage(), version) &&
                        !confirmationEvent.ask("Already on the latest version, download anyway?")) {
                        coroutineContext[Job]?.cancel() ?: throw CancellationException()
                    }
                }
                is DownloadStatus.DOWNLOADING -> {
                    progressStatus.emit("Downloading: ${type.getName()} ($version)")
                }
                is DownloadStatus.PROGRESS -> {
                    if (status.total < status.current) {
                        progressTracker.emit(ProgressTrack())
                    } else {
                        val percent = getPercentage(status.current, status.total)
                        val details = "${status.current.toSize()}/${status.total.toSize()}"
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
        val conflicted = ApkUtil.getConflictedApps(
            file, fixConflict || maskPackage
        )
        if (conflicted.isNotEmpty()) {
            if (confirmationEvent.ask(
                    "Found apps with different signatures.\n" +
                        "Please uninstall these first:\n" +
                        "[${conflicted.values.joinToString(", ")}]"
                )
            ) uninstallEvent.post(conflicted.keys)
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
