package app.neonorbit.mrvpatchmanager

import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onCompletion
import org.lsposed.patch.LSPatch
import org.lsposed.patch.OutputLogger
import java.io.File

object DefaultPatcher {
    data class Options(
        val fixConflict: Boolean, val maskPackage: Boolean,
        val fallbackMode: Boolean, val extraModules: List<String>?
    )

    fun patch(input: File, options: Options, output: File = input.out) = callbackFlow {
        var failed = false
        LSPatch.setOutputLogger(object : OutputLogger {
            override fun d(msg: String) {
                ensureActive()
                trySend(PatchStatus.PATCHING(msg))
            }
            override fun e(msg: String) {
                failed = true
                trySend(PatchStatus.FAILED(msg))
                channel.close()
            }
        })
        output.delete()
        LSPatch.main(*buildOptions(input, output, options).toTypedArray())
        if (!failed) {
            if (output.exists() && ApkUtil.verifyMrvSignature(output)) {
                send(PatchStatus.FINISHED(output))
            } else {
                output.delete()
                send(PatchStatus.FAILED("Something went wrong"))
            }
            channel.close()
        }
        awaitClose()
    }.onCompletion { error ->
        if (error != null) output.delete()
    }

    private fun buildOptions(input: File, output: File, options: Options) = ArrayList<String>().apply {
        add(input.absolutePath)
        add("--temp-dir")
        add(AppConfig.TEMP_DIR.absolutePath)
        add("--out-file")
        add(output.absolutePath)
        add("--force")
        if (options.fallbackMode) add("--fallback")
        // TODO 
    }

    private val File.out: File get() = File(AppConfig.PATCHED_OUT_DIR, this.name)

    sealed class PatchStatus {
        data class PATCHING(val msg: String) : PatchStatus()
        data class FINISHED(val file: File) : PatchStatus()
        data class FAILED(val msg: String) : PatchStatus()
    }
}
