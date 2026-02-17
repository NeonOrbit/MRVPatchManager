package app.neonorbit.mrvpatchmanager.network

import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import app.neonorbit.mrvpatchmanager.ui.CloudflareDialog
import app.neonorbit.mrvpatchmanager.util.Utils.LOG
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CloudflareInterceptor : Interceptor {
    class CfLock {
        var latch = CountDownLatch(1)
        fun reset() { latch = CountDownLatch(1) }
        fun release() { latch.countDown() }
        fun await() = latch.await(1, TimeUnit.MINUTES)
    }

    private val lock = CfLock()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val response = chain.proceed(request)
        // https://developers.cloudflare.com/cloudflare-challenges/challenge-types/challenge-pages/detect-response/
        if (response.header("cf-mitigated")?.contains("challenge", true) == true) {
            response.close()
            val cookieManager = CookieManager.getInstance()
            val staleCookie = cookieManager.getCookie(url)
            synchronized(lock) {
                val currentCookie = cookieManager.getCookie(url)
                if (currentCookie != staleCookie && currentCookie?.contains("cf_clearance") == true) {
                    return chain.proceed(request)
                }
                lock.reset()
                Handler(Looper.getMainLooper()).post {
                    CloudflareDialog.show(lock, url, staleCookie)
                }
                lock.await()
                val cookie = cookieManager.getCookie(url)
                if (cookie?.contains("cf_clearance") == true) {
                    cookie.LOG("Resuming with cf_clearance [${request.url}]")
                    return chain.proceed(request)
                } else {
                    throw IOException("Cloudflare challenge failed")
                }
            }
        }
        return response
    }
}
