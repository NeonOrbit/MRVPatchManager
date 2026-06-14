package app.neonorbit.mrvpatchmanager.ui

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.DialogFragment
import app.neonorbit.mrvpatchmanager.AppServices
import app.neonorbit.mrvpatchmanager.MainActivity
import app.neonorbit.mrvpatchmanager.network.CloudflareInterceptor
import app.neonorbit.mrvpatchmanager.network.RetrofitClient
import app.neonorbit.mrvpatchmanager.util.Utils
import app.neonorbit.mrvpatchmanager.util.Utils.LOG

class CloudflareDialog(private val lock: CloudflareInterceptor.CfLock) : DialogFragment() {
    companion object {
        private const val URL_ARG = "url"
        private const val STALE_ARG = "stale"
        fun show(lock: CloudflareInterceptor.CfLock, url: String, staleCookie: String?) {
            checkWebViewSupport()
            val fragment = CloudflareDialog(lock).apply {
                arguments = Bundle().apply {
                    putString(URL_ARG, url)
                    putString(STALE_ARG, staleCookie)
                }
            }
            fragment.show(MainActivity.current.supportFragmentManager, "cloudflare_solver")
        }
        private fun checkWebViewSupport() {
            try {
                if (WebView.getCurrentWebViewPackage()?.versionName?.split(".")?.
                    firstOrNull()?.toIntOrNull()?.let { it < 120 } == true) {
                    AppServices.showToast("Please update WebView (Android System WebView)", true)
                }
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val url = arguments?.getString(URL_ARG) ?: throw IllegalArgumentException("no url")
        val stale = arguments?.getString(STALE_ARG)
        return WebView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                userAgentString = RetrofitClient.USER_AGENT
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    try {
                        CookieManager.getInstance().flush()
                        CookieManager.getInstance().getCookie(url).let { cookie ->
                            if (isClearanceUpdated(stale, cookie)) {
                                cookie.LOG("Cloudflare cookies obtained")
                                lock.release()
                                dismiss()
                            }
                        }
                    } catch (e: Exception) {
                        Utils.warn("[CloudflareDialog] Cookie error", e)
                        lock.release()
                        dismiss()
                    }
                }
            }
        }.also {
            it.loadUrl(url)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(MATCH_PARENT, MATCH_PARENT)
    }

    override fun onCancel(dialog: DialogInterface) {
        lock.release()
        super.onCancel(dialog)
    }

    override fun onDestroy() {
        lock.release()
        super.onDestroy()
    }

    private fun isClearanceUpdated(stale: String?, current: String?): Boolean {
        val pref = "cf_clearance="
        if (current?.contains(pref) != true) return false
        if (stale?.contains(pref) != true) return true
        return current.split(";").any { c ->
            c.trim().let {
                (it.length > pref.length) && it !in stale
            }
        }
    }
}
