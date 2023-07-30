package app.neonorbit.mrvpatchmanager.keystore

import android.content.pm.Signature
import app.neonorbit.mrvpatchmanager.error
import java.io.File
import java.io.IOException
import java.security.KeyStore
import java.security.UnrecoverableKeyException

object KeystoreManager {
    fun getVerifiedData(file: File, password: String, keyAlias: String?, aliasPassword: String?): KeystoreData {
        try {
            var sig: String? = null
            var alias: String? = keyAlias
            var alsPass: String? = aliasPassword
            val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
            file.inputStream().use { input ->
                keystore.load(input, password.toCharArray())
                if (alias?.isNotBlank() != true) {
                    alias = keystore.aliases().nextElement()
                }
                if (alsPass?.isNotEmpty() != true) {
                    alsPass = password
                }
                if (!keystore.containsAlias(alias!!)) {
                    throw Exception("Wrong key alias: $alias")
                }
                try {
                    keystore.getKey(alias!!, alsPass!!.toCharArray()) ?: throw Exception(
                        "Failed to retrieve key entry!"
                    )
                    keystore.getCertificate(alias!!)?.encoded?.let {
                        sig = Signature(it).toCharsString()
                    } ?: throw Exception("Failed to retrieve key signature!")
                } catch (e: UnrecoverableKeyException) {
                    throw Exception("Wrong alias password!")
                }
            }
            return KeystoreData(file.absolutePath, password, alias!!, alsPass!!, sig!!)
        } catch (e: IOException) {
            throw when {
                e.message?.contains("KeyStore integrity check failed") == true -> "Wrong keystore password!"
                e.message?.contains("Wrong version") == true  -> "Unsupported keystore file!"
                else -> e.error
            }.let { Exception(it) }
        }
    }
}
