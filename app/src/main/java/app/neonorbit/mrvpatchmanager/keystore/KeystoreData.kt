package app.neonorbit.mrvpatchmanager.keystore

import com.google.gson.Gson

data class KeystoreData(
    val path: String,
    val password: String,
    val aliasName: String,
    val aliasPassword: String,
    val keySignature: String
) {
    fun toJson(): String = Gson().toJson(this)
}
