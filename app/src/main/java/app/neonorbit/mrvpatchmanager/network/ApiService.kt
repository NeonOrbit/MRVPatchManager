package app.neonorbit.mrvpatchmanager.network

import app.neonorbit.mrvpatchmanager.network.marker.HtmlMarker
import app.neonorbit.mrvpatchmanager.network.marker.JsonMarker
import app.neonorbit.mrvpatchmanager.network.marker.XmlMarker
import app.neonorbit.mrvpatchmanager.remote.data.ApkComboReleaseData
import app.neonorbit.mrvpatchmanager.remote.data.ApkComboVariantData
import app.neonorbit.mrvpatchmanager.remote.data.ApkFlashReleaseData
import app.neonorbit.mrvpatchmanager.remote.data.ApkFlashVariantData
import app.neonorbit.mrvpatchmanager.remote.data.ApkMirrorIFormData
import app.neonorbit.mrvpatchmanager.remote.data.ApkMirrorItemData
import app.neonorbit.mrvpatchmanager.remote.data.ApkMirrorReleaseData
import app.neonorbit.mrvpatchmanager.remote.data.ApkMirrorVariantData
import app.neonorbit.mrvpatchmanager.remote.data.ApkPureReleaseData
import app.neonorbit.mrvpatchmanager.remote.data.ApkPureVariantData
import app.neonorbit.mrvpatchmanager.remote.data.GithubReleaseData
import app.neonorbit.mrvpatchmanager.remote.data.ApkMirrorRssFeedData
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.HeaderMap
import retrofit2.http.Headers
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ApiService {
    @HEAD
    suspend fun head(@Url url: String): Response<Void>

    @GET
    suspend fun get(@Url url: String): Response<ResponseBody>

    @GET
    @Streaming
    @Headers("Cache-Control: no-store")
    suspend fun download(
        @Url directDownloadUrl: String,
        @HeaderMap headers: Map<String, String> = mapOf()
    ): Response<ResponseBody>

    @GET
    @JsonMarker
    suspend fun getGithubRelease(@Url url: String): Response<GithubReleaseData>

    @GET
    @XmlMarker
    suspend fun getApkMirrorFeed(@Url url: String): Response<ApkMirrorRssFeedData>

    @GET
    @HtmlMarker
    suspend fun getApkMirrorRelease(@Url url: String): Response<ApkMirrorReleaseData>

    @GET
    @HtmlMarker
    suspend fun getApkMirrorVariant(@Url url: String): Response<ApkMirrorVariantData>

    @GET
    @HtmlMarker
    suspend fun getApkMirrorItem(@Url url: String): Response<ApkMirrorItemData>

    @GET
    @HtmlMarker
    suspend fun getApkMirrorInputForm(@Url url: String): Response<ApkMirrorIFormData>

    @GET
    @HtmlMarker
    suspend fun getApkComboRelease(@Url url: String): Response<ApkComboReleaseData>

    @GET
    @HtmlMarker
    suspend fun getApkComboVariant(@Url url: String): Response<ApkComboVariantData>

    @GET
    @HtmlMarker
    suspend fun getApkFlashRelease(@Url url: String): Response<ApkFlashReleaseData>

    @GET
    @HtmlMarker
    suspend fun getApkFlashVariant(@Url url: String): Response<ApkFlashVariantData>

    @GET
    @HtmlMarker
    suspend fun getApkPureRelease(@Url url: String): Response<ApkPureReleaseData>

    @GET
    @HtmlMarker
    suspend fun getApkPureVariant(@Url url: String): Response<ApkPureVariantData>
}
