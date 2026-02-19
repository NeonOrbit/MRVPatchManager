package app.neonorbit.mrvpatchmanager

import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import app.neonorbit.mrvpatchmanager.keystore.KeystoreData
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onCompletion
import org.lsposed.patch.MRVPatcher
import org.lsposed.patch.OutputLogger
import java.io.File

class DefaultPatcher(private val input: File, private val options: Options) {
    private val output: File by lazy {
        File(AppConfigs.PATCHED_OUT_DIR, input.name)
    }

    fun patch() = callbackFlow {
        checkPreconditions()
        initStatusProducer(this)
        MRVPatcher.patch(*buildOptions())
        send(PatchStatus.PATCHING("verifying apk"))
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
        MRVPatcher.setLogger(object : OutputLogger {
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

    private fun File.verify() = this.exists() && ApkUtil.verifySignature(
        this, options.customKeystore?.keySignature ?: AppConfigs.MRV_PUBLIC_SIGNATURE
    )

    private fun buildOptions() = ArrayList<String>(17).apply {
        add(input.absolutePath)
        add("--internal-patch")
        add("--temp-dir")
        add(AppConfigs.TEMP_DIR.absolutePath)
        add("--out-file")
        add(output.absolutePath)
        add("--force")
        if (options.fixConflict) add("--fix-conf")
        if (options.maskPackage) add("--mask-pkg")
        if (options.fallbackMode) add("--fallback")
        options.customKeystore?.let { data ->
            add("--key-args")
            add(data.path)
            add(data.password)
            add(data.aliasName)
            add(data.aliasPassword)
        }
        options.extraModules?.let { mods ->
            add("--modules")
            mods.forEach { add(it) }
        }
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
