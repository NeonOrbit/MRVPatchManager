package app.neonorbit.mrvpatchmanager.data

import androidx.annotation.DrawableRes

data class AppItemData(val name: String, val type: AppType, @param:DrawableRes val icon: Int = -1) {
    override fun toString(): String {
        return name
    }
}
