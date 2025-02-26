package app.neonorbit.mrvpatchmanager.ui.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neonorbit.mrvpatchmanager.AppConfigs
import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.DefaultPatcher
import app.neonorbit.mrvpatchmanager.DefaultPatcher.PatchStatus
import app.neonorbit.mrvpatchmanager.DefaultPreference
import app.neonorbit.mrvpatchmanager.R
import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import app.neonorbit.mrvpatchmanager.compareVersion
import app.neonorbit.mrvpatchmanager.data.AppFileData
import app.neonorbit.mrvpatchmanager.data.AppType
import app.neonorbit.mrvpatchmanager.download.DownloadStatus
import app.neonorbit.mrvpatchmanager.error
import app.neonorbit.mrvpatchmanager.event.ConfirmationEvent
import app.neonorbit.mrvpatchmanager.event.SingleEvent
import app.neonorbit.mrvpatchmanager.post
import app.neonorbit.mrvpatchmanager.remote.GithubService
import app.neonorbit.mrvpatchmanager.repository.ApkRepository
import app.neonorbit.mrvpatchmanager.toSizeString
import app.neonorbit.mrvpatchmanager.toTempFile
import app.neonorbit.mrvpatchmanager.util.Utils
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
import java.util.StringJoiner
import kotlin.coroutines.coroutineContext

class HomeViewModel : ViewModel() {
    private val repository = ApkRepository()
    val fbAppList = AppConfigs.FB_APP_LIST

    val uriEvent = SingleEvent<Uri>()
    val intentEvent = SingleEvent<Intent>()
    val messageEvent = SingleEvent<String>()
    val installEvent = SingleEvent<File>()
    val uninstallEvent = SingleEvent<Set<String>>()
    val apkPickerEvent = SingleEvent<Intent>()
    val appPickerEvent = SingleEvent<List<AppFileData>>()
    val confirmationEvent = ConfirmationEvent()

    val patchingStatus = MutableStateFlow(false)
    val progressStatus = MutableStateFlow<String?>(null)
    val progressTracker = MutableStateFlow(ProgressTrack())

    val quickDownloadJob = MutableStateFlow<Job?>(null)
    val quickDownloadProgress = MutableStateFlow<Int?>(null)

    var lastSelectedVersion: String? = null; private set

    private var moduleLatest: String? = null
    val moduleStatus: MutableStateFlow<VersionStatus> by lazy {
        MutableStateFlow(VersionStatus(null, null))
    }

    private fun getPatcherOptions() = DefaultPatcher.Options(
        fixConflict = DefaultPreference.isFixConflictEnabled(),
        maskPackage = DefaultPreference.isPackageMaskEnabled(),
        fallbackMode = DefaultPreference.isFallbackModeEnabled(),
        customKeystore = DefaultPreference.getCustomKeystore(),
        extraModules = DefaultPreference.getExtraModules()
    )
    private var currentOptions: DefaultPatcher.Options = getPatcherOptions()
    private val preferredAbi: String get() = DefaultPreference.getPreferredABI()
    val hideModuleWarn get() = DefaultPreference.getExtraModules() != null

    private var skipCheck = false
    private var patchingJob: Job? = null

    private val moduleInfoIntent: Intent by lazy {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", AppConfigs.MODULE_PACKAGE, null)
        )
    }

    private val filePickerIntent: Intent by lazy {
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = ApkConfigs.APK_MIME_TYPE
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

    fun loadModuleStatus(reload: Boolean = false) {
        if (reload) GithubService.checkForUpdate()
        if (moduleStatus.value.current == null || reload) {
            val version = ApkUtil.getPrefixedVersionName(AppConfigs.MODULE_PACKAGE)
            if ((version?.compareVersion(moduleLatest) ?: 0) >= 0) {
                moduleLatest = null
            }
            moduleStatus.post(this, VersionStatus(version, moduleLatest))
        }
    }

    fun updateModuleStatus(current: String, latest: String) {
        if (latest != moduleLatest) {
            moduleLatest = latest
            moduleStatus.post(this, VersionStatus(current, moduleLatest))
        }
    }

    fun showModuleInfo() = intentEvent.post(viewModelScope, moduleInfoIntent)
    fun visitModule() = uriEvent.post(viewModelScope, Uri.parse(AppConfigs.MODULE_LATEST_URL))
    fun visitManager() = uriEvent.post(viewModelScope, Uri.parse(AppConfigs.MANAGER_LATEST_URL))

    fun installModule(force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO + catcher) {
            install(repository.getModuleApk(force))
        }.let { job ->
            quickDownloadJob.post(this, job)
        }
    }

    fun installManager() {
        viewModelScope.launch(Dispatchers.IO + catcher) {
            install(repository.getManagerApk())
        }.let { job ->
            quickDownloadJob.post(this, job)
        }
    }

    fun manualRequest(storage: Boolean) {
        if (patchingJob != null) {
            messageEvent.post(viewModelScope, "A patching task is already in progress.")
        } else if (storage) {
            apkPickerEvent.post(viewModelScope, filePickerIntent)
        } else {
            viewModelScope.launch {
                appPickerEvent.post(ApkUtil.getInstalledAppList())
            }
        }
    }

    fun patchVersion(app: AppType, target: String) {
        if (patchingJob != null) {
            messageEvent.post(viewModelScope, "A patching task is already in progress.")
        } else {
            target.trim().trim('"', '.').takeIf { ApkConfigs.isValidVersionString(it) }?.let {
                lastSelectedVersion = it
                patch(app = app, target = it)
            } ?: messageEvent.post(viewModelScope, "Invalid version: $target")
        }
    }

    fun patch(app: AppType? = null, uri: Uri? = null, target: String? = null) {
        if (patchingJob != null) {
            progressStatus.post(this, "Stopping...")
            patchingJob?.cancel()
            return
        }
        skipCheck = false
        var manual: File? = null
        currentOptions = getPatcherOptions()
        viewModelScope.launch(Dispatchers.Default + catcher) {
            progressStatus.emit("Progress...")
            progressTracker.emit(ProgressTrack())
            val file = uri?.toTempFile(AppServices.contentResolver)?.also {
                manual = it
            } ?: app?.takeIf { checkMaskPackage(it) }?.let {
                getPatchableApkFile(app, target)
            }
            file?.takeIf {
                checkPreconditions(file, manual != null).also {
                    if (!it) progressStatus.emit(null)
                }
            }?.let {
                progressTracker.emit(ProgressTrack())
                patchApk(file)?.let { patched ->
                    progressTracker.emit(ProgressTrack(100, percent = ""))
                    install(patched)
                }
            }
        }.let { job ->
            patchingJob = job
            patchingStatus.post(this, true)
            job.invokeOnCompletion {
                manual?.delete()
                if (it is CancellationException) {
                    viewModelScope.launch {
                        progressStatus.emit(null)
                        progressTracker.emit(ProgressTrack())
                    }
                } else if (progressTracker.value.current < 0) {
                    progressTracker.post(this, ProgressTrack(0))
                }
                patchingJob = null
                patchingStatus.post(this, false)
            }
        }
    }

    private suspend fun patchApk(input: File): File? {
        val patched = AppConfigs.getPatchedApkFile(input) ?: throw Exception(
            "Failed to retrieve apk file info"
        )
        if (!skipCheck && patched.exists() && !confirmationEvent.ask(
                "${patched.name} already exists in the patched apk list, patch again?"
            )) {
            progressStatus.emit(null)
            return null
        }
        patched.delete()
        progressStatus.emit("Patching...")
        return DefaultPatcher(input, currentOptions).patch().onEach { status ->
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
            Utils.error("Patch Error", it)
            progressStatus.emit("Patch Error: ${it.error}")
            progressTracker.emit(ProgressTrack(0))
        }.lastOrNull().let {
            if (it is PatchStatus.FINISHED) patched else null
        }
    }

    private suspend fun getPatchableApkFile(type: AppType, target: String?): File? {
        var version = "latest"
        val customSig = currentOptions.customKeystore?.keySignature
        return repository.getFbApk(type, preferredAbi, target).onEach { status ->
            when (status) {
                is DownloadStatus.FETCHING -> {
                    progressStatus.emit("Fetching: ${status.server}")
                }
                is DownloadStatus.FETCHED -> {
                    version = status.version
                    if (ApkUtil.hasLatestMrvSignedApp(type.getPackage(), version, customSig) && !confirmationEvent.ask(
                            "Already on the latest version, download anyway?"
                    ).also { skipCheck = it }) {
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
                        val details = "${status.current.toSizeString()}/${status.total.toSizeString()}"
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
            progressStatus.emit("Download Error: ${it.message}")
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
        if (conflicted.isEmpty()) {
            if (confirmationEvent.ask("Install ${file.name}?")) installEvent.post(file)
        } else if (confirmationEvent.ask(msg = "Apps with different signatures have been detected.\n" +
                    "Please uninstall the following apps first:\n" + "[${conflicted.values.joinToString(", ")}]",
                action = "Uninstall")
            ) {
            uninstallEvent.post(conflicted.keys)
            if (confirmationEvent.ask("Install ${file.name}?")) installEvent.post(file)
        }
    }

    private suspend fun checkMaskPackage(app: AppType? = null, file: File? = null): Boolean {
        if (currentOptions.maskPackage && !(ApkUtil.isMessenger(app) || ApkUtil.isMessenger(file))) {
            messageEvent.post("!!NOT ALLOWED!! 'Mask package name' option is currently enabled, " +
                    "only Messenger is allowed to be patched in this mode."
            )
            progressStatus.emit(null)
            return false
        }
        return true
    }

    private suspend fun checkPreconditions(file: File, isManual: Boolean): Boolean {
        if (!checkMaskPackage(file = file)) return false
        if (isManual && ApkUtil.isPatched(file)) {
            messageEvent.post("The selected apk has already been patched.")
            return false
        }
        val passed = !currentOptions.fixConflict || ApkUtil.isRecommendedForResolveConflicts(file) || confirmationEvent.ask(
            "'Resolve apk conflicts' option is currently enabled, " +
                    "there's no need to patch any other apps besides Facebook and Messenger. " +
                    "Patch anyway?"
        ).also { skipCheck = it }
        if (!isManual || !passed) return passed
        if (!ApkUtil.verifyFbSignature(file, false)) {
            return confirmationEvent.ask("Warning!",
                "The selected apk is either not original or has already been patched, proceed anyway?"
            ).also { skipCheck = it }
        }
        return skipCheck || !ApkUtil.hasLatestMrvSignedApp(file, currentOptions.customKeystore?.keySignature) || confirmationEvent.ask(
            "Already on the latest version, patch anyway?"
        ).also { skipCheck = it }
    }

    private fun getPercentage(current: Long, total: Long): Int {
        return if (total < current) -1 else (current * 100 / total).toInt()
    }

    data class VersionStatus(val current: String?, val latest: String? = null)

    class ProgressTrack(val current: Int = -1, val details: String = "∞", percent: String = "∞") {
        val percent = if (percent.isEmpty() || current < 0) "∞" else "$current%"
    }

    fun getNotice(): String? = getPatcherOptions().let { opt ->
        StringJoiner("\n").apply {
            if (opt.maskPackage) add(getEnabledString(R.string.pref_mask_package_title))
            if (opt.fixConflict) add(getEnabledString(R.string.pref_fix_conflict_title))
            if (opt.fallbackMode) add(getEnabledString(R.string.pref_fallback_mode_title))
            opt.extraModules?.let { mods ->
                add("[Modules] -> " + mods.joinToString(", ", "[", "]") { AppConfigs.tagMessengerPro(it) })
            }
        }.toString().takeIf { it.isNotEmpty() }
    }
    private fun getEnabledString(@StringRes resId: Int) = "[Enabled] -> [${AppServices.application.getString(resId)}]"
}
