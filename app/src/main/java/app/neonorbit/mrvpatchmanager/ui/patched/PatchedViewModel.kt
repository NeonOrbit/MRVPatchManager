package app.neonorbit.mrvpatchmanager.ui.patched

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neonorbit.mrvpatchmanager.repository.data.ApkFileData
import app.neonorbit.mrvpatchmanager.event.ConfirmationEvent
import app.neonorbit.mrvpatchmanager.launchLocking
import app.neonorbit.mrvpatchmanager.repository.ApkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import java.io.File

class PatchedViewModel : ViewModel() {
    private val mutex: Mutex = Mutex()
    private val repository = ApkRepository()

    val progressState = MutableLiveData<Boolean>()
    val confirmationEvent = ConfirmationEvent()

    val patchedApkList by lazy {
        MutableLiveData<List<ApkFileData>>()
    }

    fun reloadPatchedApks() {
        viewModelScope.launchLocking(mutex, Dispatchers.IO) {
            patchedApkList.postValue(repository.getPatchedApks())
        }
    }

    fun deletePatchedApks(items: List<ApkFileData>) {
        if (items.isEmpty()) return
        val message = "Delete ${getDeleteMsg(items)}?"
        viewModelScope.launchLocking(mutex, Dispatchers.IO) {
            if (confirmationEvent.ask("Delete", message)) {
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

    private fun getDeleteMsg(items: List<ApkFileData>): String {
        return if (items.size > 1) "${items.size} files" else items.single().name
    }
}
