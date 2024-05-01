package app.neonorbit.mrvpatchmanager.keystore

import android.content.pm.Signature
import app.neonorbit.mrvpatchmanager.error
import java.io.File
import java.io.IOException
import java.security.KeyStore
import java.security.UnrecoverableKeyException

object KeystoreManager {
    fun readKeyData(input: File, path: String, password: String, inAlias: String?, inAliasPass: String?): KeystoreData {
        try {
            var sig: String? = null
            var alias: String? = inAlias
            var aliasPass: String? = inAliasPass
            val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
            input.inputStream().use { stream ->
                keystore.load(stream, password.toCharArray())
                if (alias?.isNotBlank() != true) {
                    alias = keystore.aliases().nextElement()
                }
                if (aliasPass?.isNotEmpty() != true) {
                    aliasPass = password
                }
                if (!keystore.containsAlias(alias!!)) {
                    throw Exception("Wrong key alias: $alias")
                }
                try {
                    keystore.getKey(alias!!, aliasPass!!.toCharArray()) ?: throw Exception(
                        "Failed to retrieve key entry!"
                    )
                    keystore.getCertificate(alias!!)?.encoded?.let {
                        sig = Signature(it).toCharsString()
                    } ?: throw Exception("Failed to retrieve key signature!")
                } catch (e: UnrecoverableKeyException) {
                    throw Exception("Wrong alias password!")
                }
            }
            return KeystoreData(path, password, alias!!, aliasPass!!, sig!!)
        } catch (e: IOException) {
            throw when {
                e.message?.contains("KeyStore integrity check failed") == true -> "Wrong keystore password!"
                e.message?.contains("Wrong version") == true  -> "Unsupported keystore file!"
                else -> e.error
            }.let { Exception(it) }
        }
    }
}
