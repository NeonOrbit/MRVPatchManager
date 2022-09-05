package app.neonorbit.mrvpatchmanager.network

import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.BuildConfig
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.File
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://place.holder/"
    private const val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64)"
    private const val CACHE_PATH = "http-cache"
    private const val MAX_CACHE_HOUR = 1
    private const val MAX_CACHE_SIZE = 1024 * 1024 * 10L

    val SERVICE: ApiService by lazy {
        CLIENT.create(ApiService::class.java)
    }

    private val CLIENT: Retrofit by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.request().newBuilder()
                    .header(HttpSpec.Header.USER_AGENT, USER_AGENT)
                    .build().let { request ->
                        chain.proceed(request)
                    }
            }.addNetworkInterceptor { chain ->
                chain.request().let { request ->
                    chain.proceed(request).newBuilder()
                        .header(HttpSpec.Header.CACHE_CONTROL, cacheHeader(request))
                        .build()
                }
            }.also {
                if (BuildConfig.DEBUG) it.addNetworkInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.HEADERS
                    }
                )
            }
            .cache(httpCache)
            .readTimeout(50, TimeUnit.SECONDS)
            .connectTimeout(40, TimeUnit.SECONDS)
            .build().let { okHttp ->
                Retrofit.Builder()
                    .addConverterFactory(ConverterFactory())
                    .baseUrl(BASE_URL)
                    .client(okHttp)
                    .build()
            }
    }

    private val httpCache: Cache by lazy {
        Cache(File(AppServices.getCacheDir(), CACHE_PATH), MAX_CACHE_SIZE)
    }

    private fun cacheHeader(request: Request): String {
        return if (request.cacheControl.noStore) request.cacheControl.toString()
        else CacheControl.Builder().maxAge(MAX_CACHE_HOUR, TimeUnit.HOURS).build().toString()
    }
}
