package app.neonorbit.mrvpatchmanager.data

import androidx.annotation.DrawableRes

data class AppItemData(val name: String, val type: AppType, @DrawableRes val icon: Int = -1) {
    override fun toString(): String {
        return name
    }
}
