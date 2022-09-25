package app.neonorbit.mrvpatchmanager.remote.data

import com.google.gson.annotations.SerializedName

data class GithubReleaseData(
    @field:SerializedName("tag_name")
    val version: String,
    @field:SerializedName("assets")
    val assets: List<Asset>
) {
    data class Asset(
        @field:SerializedName("name")
        val name: String,
        @field:SerializedName("browser_download_url")
        val link: String,
    )
}
