package app.neonorbit.mrvpatchmanager.keystore

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class KeystoreInputData(
    val uri: Uri,
    val password: String,
    val aliasName: String?,
    val aliasPassword: String?
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        uri.writeToParcel(parcel, flags)
        parcel.writeString(password)
        parcel.writeString(aliasName)
        parcel.writeString(aliasPassword)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<KeystoreInputData> {
        override fun createFromParcel(parcel: Parcel): KeystoreInputData {
            return KeystoreInputData(
                Uri.CREATOR.createFromParcel(parcel),
                parcel.readString()!!,
                parcel.readString(),
                parcel.readString()
            )
        }

        override fun newArray(size: Int): Array<KeystoreInputData?> {
            return arrayOfNulls(size)
        }
    }
}
