package app.neonorbit.mrvpatchmanager.network

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
}
