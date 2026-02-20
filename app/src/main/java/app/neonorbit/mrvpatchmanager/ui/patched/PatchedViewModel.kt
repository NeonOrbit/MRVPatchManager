package app.neonorbit.mrvpatchmanager.ui.patched

import android.content.Intent
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.UniversalInstaller
import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import app.neonorbit.mrvpatchmanager.error
import app.neonorbit.mrvpatchmanager.event.ConfirmationEvent
import app.neonorbit.mrvpatchmanager.event.SingleEvent
import app.neonorbit.mrvpatchmanager.existAnyIn
import app.neonorbit.mrvpatchmanager.launchSyncedBlock
import app.neonorbit.mrvpatchmanager.repository.ApkRepository
import app.neonorbit.mrvpatchmanager.repository.data.ApkFileData
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.io.File

class PatchedViewModel : ViewModel() {
    private val mutex: Mutex = Mutex()
    private val repository = ApkRepository()

    val intentEvent = SingleEvent<Intent>()
    val dialogEvent = SingleEvent<String>()
    val saveFilesEvent = SingleEvent<Intent>()
    val confirmationEvent = ConfirmationEvent()
    val progressState = MutableLiveData<Boolean>()

    val patchedApkList by lazy {
        MutableLiveData<List<ApkFileData>>()
    }

    private val pendingSaveFiles by lazy {
        ArrayList<String>()
    }

    fun reloadPatchedApks() {
        viewModelScope.launchSyncedBlock(mutex, Dispatchers.IO) {
            patchedApkList.postValue(repository.getPatchedApks())
        }
    }

    fun saveSelectedApks(items: List<ApkFileData>) {
        if (items.isEmpty()) return
        pendingSaveFiles.clear()
        val message = "Save ${getSelectedFilesMsg(items)}?"
        viewModelScope.launch {
            if (confirmationEvent.ask(null, message, "Save")) {
                pendingSaveFiles.addAll(items.map { it.path })
                saveFilesEvent.post(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
            }
        }
    }

    fun onSaveDirectoryPicked(intent: Intent) {
        if (pendingSaveFiles.isEmpty()) return
        val outDir = intent.data?.let { AppServices.resolveDocumentTree(it)}
        if (outDir != null) {
            saveFilesToDisk(outDir, pendingSaveFiles.map { File(it) })
        } else {
            pendingSaveFiles.clear()
            viewModelScope.launch(Dispatchers.Main) {
                AppServices.showToast("Failed to resolve output directory")
            }
        }
    }

    fun shareSelectedApks(items: List<ApkFileData>) {
        if (items.isEmpty()) return
        viewModelScope.launchSyncedBlock(mutex, Dispatchers.IO) {
            val uris = items.mapNotNull {
                AppServices.resolveContentUri(File(it.path))
            }.let { ArrayList(it) }
            intentEvent.post(Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = ApkConfigs.APK_MIME_TYPE
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            })
        }
    }

    fun deleteSelectedApks(items: List<ApkFileData>) {
        if (items.isEmpty()) return
        val message = "Delete ${getSelectedFilesMsg(items)}?"
        viewModelScope.launchSyncedBlock(mutex, Dispatchers.IO) {
            if (confirmationEvent.ask(null, message, "Delete")) {
                progressState.postValue(true)
                items.forEach {
                    File(it.path).delete()
                }
                reloadPatchedApks()
            }
        }.invokeOnCompletion {
            progressState.postValue(false)
        }
    }

    private fun saveFilesToDisk(outDir: DocumentFile, files: List<File>) {
        val catcher = CoroutineExceptionHandler { _, it ->
            viewModelScope.launch(Dispatchers.Main) {
                AppServices.showToast(it.error)
            }
        }
        viewModelScope.launchSyncedBlock(mutex, Dispatchers.IO + catcher) {
            if (files.existAnyIn(outDir) && !(confirmationEvent.ask(
                    "One or more files already exist. Overwrite?"
                ))) {
                throw Exception("Cancelled")
            }
            progressState.postValue(true)
            files.forEach { srcFile ->
                val outFile = outDir.findFile(srcFile.name) ?: outDir.createFile(
                    ApkConfigs.APK_MIME_TYPE, srcFile.name
                ) ?: throw Exception("Failed to create file")
                AppServices.contentResolver.openOutputStream(outFile.uri)?.use { output ->
                    srcFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                } ?: throw Exception("Failed to open output stream")
            }
        }.invokeOnCompletion { e ->
            progressState.postValue(false)
            if (e == null) {
                viewModelScope.launch(Dispatchers.Main) {
                    AppServices.showToast("Saved")
                }
            }
        }
    }

    fun showDetails(items: List<ApkFileData>) {
        if (items.isEmpty()) return
        viewModelScope.launchSyncedBlock(mutex, Dispatchers.IO) {
            progressState.postValue(true)
            ApkUtil.getApkDetails(items.take(4).map { File(it.path) }).let {
                dialogEvent.post(it)
            }
        }.invokeOnCompletion {
            progressState.postValue(false)
        }
    }

    fun installApk(item: ApkFileData) {
        if (mutex.isLocked) return
        val catcher = CoroutineExceptionHandler { _, it ->
            viewModelScope.launch(Dispatchers.Main) {
                AppServices.showToast(it.error)
            }
        }
        viewModelScope.launchSyncedBlock(mutex, Dispatchers.IO + catcher) {
            progressState.postValue(true)
            val file = File(item.path)
            val conflicted = ApkUtil.getConflictedApps(file)
            if (conflicted.isEmpty() || confirmationEvent.ask(null,
                    "Installation may fail due to conflicting apk signatures with the following apps:\n" +
                            "[${conflicted.values.joinToString(", ")}]",
                    "Install Anyway"
                )) {
                UniversalInstaller.install(file)
            }
        }.invokeOnCompletion {
            progressState.postValue(false)
        }
    }

    private fun getSelectedFilesMsg(items: List<ApkFileData>): String {
        return if (items.size > 1) "${items.size} files" else items.single().name
    }
}
