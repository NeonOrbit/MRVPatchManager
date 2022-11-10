package app.neonorbit.mrvpatchmanager

import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.lsposed.patch.LSPatch
import org.lsposed.patch.OutputLogger
import java.io.File

object DefaultPatcher {
    fun patch(input: File, fallback: Boolean) = callbackFlow {
        var failed = false
        LSPatch.setOutputLogger(object : OutputLogger {
            override fun v(msg: String) { }
            override fun d(msg: String) {
                trySend(PatchStatus.PATCHING(msg))
            }
            override fun e(msg: String) {
                failed = true
                trySend(PatchStatus.FAILED(msg))
                channel.close()
            }
        })
        val output = File(AppConfig.PATCHED_OUT_DIR, input.name).also {
            it.delete()
        }
        LSPatch.main(*buildOptions(input, output, fallback).toTypedArray())
        if (!failed) {
            if (output.exists() && ApkUtil.verifyMrvSignature(output)) {
                send(PatchStatus.FINISHED(output))
            } else {
                output.delete()
                send(PatchStatus.FAILED("Something went wrong"))
            }
            close()
        }
        awaitClose()
    }

    private fun buildOptions(input: File, output: File, fallback: Boolean) =
        ArrayList<String>().apply {
            add(input.absolutePath)
            add("--temp-dir")
            add(AppConfig.TEMP_DIR.absolutePath)
            add("--out-file")
            add(output.absolutePath)
            add("--force")
            if (fallback) add("--fallback")
        }

    sealed class PatchStatus {
        data class PATCHING(val msg: String) : PatchStatus()
        data class FINISHED(val file: File) : PatchStatus()
        data class FAILED(val msg: String) : PatchStatus()
    }
}
