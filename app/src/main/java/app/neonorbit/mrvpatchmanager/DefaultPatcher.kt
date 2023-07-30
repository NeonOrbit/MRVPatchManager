package app.neonorbit.mrvpatchmanager

import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import app.neonorbit.mrvpatchmanager.keystore.KeystoreData
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onCompletion
import org.lsposed.patch.LSPatch
import org.lsposed.patch.OutputLogger
import java.io.File

class DefaultPatcher(private val input: File, private val options: Options) {
    private val output: File by lazy {
        File(AppConfig.PATCHED_OUT_DIR, input.name)
    }

    fun patch() = callbackFlow {
        checkPreconditions()
        initStatusProducer(this)
        LSPatch.main(*buildOptions())
        if (output.verify()) {
            send(PatchStatus.FINISHED(output))
        } else {
            output.delete()
            send(PatchStatus.FAILED("Something went wrong"))
        }
        channel.close()
        awaitClose()
    }.onCompletion { error ->
        if (error != null) output.delete()
    }

    private fun initStatusProducer(scope: ProducerScope<Any>) {
        LSPatch.setOutputLogger(object : OutputLogger {
            override fun d(message: String) {
                scope.ensureActive()
                scope.trySend(PatchStatus.PATCHING(message))
            }

            override fun e(error: String) {
                throw Exception(error)
            }
        })
    }

    private fun checkPreconditions() {
        output.delete()
        if (output.exists()) throw Exception(
            "Failed to delete output file"
        )
    }

    private fun File.verify() = output.exists() && ApkUtil.verifySignature(
        this, options.customKeystore?.keySignature ?: AppConfig.MRV_PUBLIC_SIGNATURE
    )

    private fun buildOptions() = ArrayList<String>(15).apply {
        add(input.absolutePath)
        add("--temp-dir")
        add(AppConfig.TEMP_DIR.absolutePath)
        add("--out-file")
        add(output.absolutePath)
        add("--force")
    }.toTypedArray()

    sealed class PatchStatus {
        data class PATCHING(val msg: String) : PatchStatus()
        data class FINISHED(val file: File) : PatchStatus()
        data class FAILED(val msg: String) : PatchStatus()
    }

    data class Options(
        val fixConflict: Boolean, val maskPackage: Boolean, val fallbackMode: Boolean,
        val customKeystore: KeystoreData?, val extraModules: List<String>?
    )
}
