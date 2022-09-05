package app.neonorbit.mrvpatchmanager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import androidx.annotation.WorkerThread
import java.net.InetSocketAddress
import java.net.Socket

object SystemServices {
    fun getNetworkService(context: Context): ConnectivityManager? {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    }

    object Network {
        private fun isConnected(context: Context): Boolean {
            return getNetworkService(context)?.let {
                it.getNetworkCapabilities(it.activeNetwork)
            }?.let {
                it.hasCapability(NET_CAPABILITY_INTERNET) &&
                    it.hasCapability(NET_CAPABILITY_VALIDATED)
            } == true
        }

        @WorkerThread
        fun isOnline(context: Context): Boolean {
            if (!isConnected(context)) return false
            return try {
                Socket().use {
                    it.connect(
                        InetSocketAddress("8.8.8.8", 53), 5000
                    )
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
