package app.neonorbit.mrvpatchmanager.network

import app.neonorbit.mrvpatchmanager.network.parser.HtmlParser
import app.neonorbit.mrvpatchmanager.network.parser.JsonParser
import app.neonorbit.mrvpatchmanager.network.parser.XmlParser
import app.neonorbit.mrvpatchmanager.remote.data.ApkComboReleaseData
import app.neonorbit.mrvpatchmanager.remote.data.ApkComboVariantData
import app.neonorbit.mrvpatchmanager.remote.data.ApkFlashReleaseData
import app.neonorbit.mrvpatchmanager.remote.data.ApkFlashVariantData
import app.neonorbit.mrvpatchmanager.remote.data.ApkMirrorItemData
import app.neonorbit.mrvpatchmanager.remote.data.ApkMirrorIFormData
import app.neonorbit.mrvpatchmanager.remote.data.ApkMirrorReleaseData
import app.neonorbit.mrvpatchmanager.remote.data.ApkMirrorVariantData
import app.neonorbit.mrvpatchmanager.remote.data.ApkPureItemData
import app.neonorbit.mrvpatchmanager.remote.data.ApkPureReleaseData
import app.neonorbit.mrvpatchmanager.remote.data.ApkPureVariantData
import app.neonorbit.mrvpatchmanager.remote.data.GithubReleaseData
import app.neonorbit.mrvpatchmanager.remote.data.RssFeedData
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
    @XmlParser
    suspend fun getRssFeed(@Url url: String): Response<RssFeedData>

    @GET
    @JsonParser
    suspend fun getGithubRelease(@Url url: String): Response<GithubReleaseData>

    @GET
    @HtmlParser
    suspend fun getApkMirrorRelease(@Url url: String): Response<ApkMirrorReleaseData>

    @GET
    @HtmlParser
    suspend fun getApkMirrorVariant(@Url url: String): Response<ApkMirrorVariantData>

    @GET
    @HtmlParser
    suspend fun getApkMirrorItem(@Url url: String): Response<ApkMirrorItemData>

    @GET
    @HtmlParser
    suspend fun getApkMirrorInputForm(@Url url: String): Response<ApkMirrorIFormData>

    @GET
    @HtmlParser
    suspend fun getApkComboRelease(@Url url: String): Response<ApkComboReleaseData>

    @GET
    @HtmlParser
    suspend fun getApkComboVariant(@Url url: String): Response<ApkComboVariantData>

    @GET
    @HtmlParser
    suspend fun getApkFlashRelease(@Url url: String): Response<ApkFlashReleaseData>

    @GET
    @HtmlParser
    suspend fun getApkFlashVariant(@Url url: String): Response<ApkFlashVariantData>

    @GET
    @HtmlParser
    suspend fun getApkPureRelease(@Url url: String): Response<ApkPureReleaseData>

    @GET
    @HtmlParser
    suspend fun getApkPureVariant(@Url url: String): Response<ApkPureVariantData>

    @GET
    @HtmlParser
    suspend fun getApkPureItem(@Url url: String): Response<ApkPureItemData>
}
