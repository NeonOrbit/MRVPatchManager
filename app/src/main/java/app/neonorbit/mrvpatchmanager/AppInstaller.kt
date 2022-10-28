package app.neonorbit.mrvpatchmanager

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.core.content.FileProvider
import org.greenrobot.eventbus.EventBus
import java.io.File

object AppInstaller {
    data class Event(val pkg: String)
    private const val DATA = "package:app.neonorbit."
    private const val MIME = "application/vnd.android.package-archive"
    private const val AUTH = "${BuildConfig.APPLICATION_ID}.file.provider"

    private val receiver by lazy {
        object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.dataString?.takeIf { it.startsWith(DATA) }?.let {
                    EventBus.getDefault().postSticky(Event(it.substringAfter(":")))
                }
            }
        }
    }

    fun register(application: Application) {
        application.registerReceiver(receiver, IntentFilter().apply {
            addDataScheme("package")
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
        })
    }

    @Suppress("deprecation")
    fun install(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(FileProvider.getUriForFile(context, AUTH, file), MIME)
        }
        context.startActivity(intent)
    }

    @Suppress("deprecation")
    fun uninstall(context: Context, pkg: String) {
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
            data = Uri.fromParts("package", pkg, null)
        }
        context.startActivity(intent)
    }

    /** TO-DO: Migrate to PackageInstaller **/
}
