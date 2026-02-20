package app.neonorbit.mrvpatchmanager

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import app.neonorbit.mrvpatchmanager.apk.ApkConfigs
import org.greenrobot.eventbus.EventBus
import java.io.File

object AppInstaller {
    data class Event(val pkg: String)

    private const val VALID_PKG = "package:app.neonorbit."

    private val receiver by lazy {
        object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.dataString?.takeIf { it.startsWith(VALID_PKG) }?.let {
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

    @Suppress("Deprecation")
    @SuppressLint("RequestInstallPackagesPolicy")
    fun install(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(AppServices.resolveContentUri(file), ApkConfigs.APK_MIME_TYPE)
        }
        context.launch(intent)
    }

    @Suppress("Deprecation")
    fun uninstall(context: Context, pkg: String) {
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
            data = Uri.fromParts("package", pkg, null)
        }
        context.launch(intent)
    }

    private fun Context.launch(intent: Intent) {
        if (this !is Activity)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /** TO-DO: Migrate to PackageInstaller **/
}
