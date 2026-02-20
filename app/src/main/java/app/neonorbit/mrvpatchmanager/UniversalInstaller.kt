package app.neonorbit.mrvpatchmanager

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.FileProvider
import app.neonorbit.mrvpatchmanager.apk.ApkUtil
import app.neonorbit.mrvpatchmanager.util.Utils
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile

object UniversalInstaller {
    data class Event(val msg: String? = null, val intent: Intent? = null)
    private const val ACTION_INSTALL = ".ACTION_INSTALL_STATUS"
    private val currentSession: AtomicInteger = AtomicInteger(-1)

    class InstallationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, 0) != currentSession.get()) return
            EventBus.getDefault().postSticky(Event())
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    @Suppress("Deprecation")
                    intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        EventBus.getDefault().postSticky(Event("Installing", intent = this))
                    }
                }
                PackageInstaller.STATUS_SUCCESS -> {
                    AppServices.showToast("Installed Successfully")
                }
                PackageInstaller.STATUS_FAILURE_ABORTED -> {
                    AppServices.showToast("Installation cancelled")
                }
                else -> {
                    val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    AppServices.showToast("Install failed: $msg", long = true)
                    Utils.error("Install failed: $msg")
                }
            }
            if (status != PackageInstaller.STATUS_PENDING_USER_ACTION) currentSession.set(-1)
        }
    }

    @SuppressLint("RequestInstallPackagesPolicy")
    fun install(file: File) {
        EventBus.getDefault().postSticky(Event("Preparing"))
        val context = AppServices.application
        try {
            val installer = context.packageManager.packageInstaller
            clearAbandonedSessions(installer)
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = installer.createSession(params).also { currentSession.set(it) }
            installer.openSession(sessionId).use { session ->
                if (ApkUtil.isApk(file, verify = true)) {
                    writeFileToSession(session, file.inputStream(), "base.apk", file.length())
                } else {
                    ZipFile(file).use { zip ->
                        zip.entries().asSequence().forEach { entry ->
                            if (entry.name.endsWith(".apk") && !entry.isDirectory) {
                                writeFileToSession(session, zip.getInputStream(entry), entry.name, entry.size)
                            }
                        }
                    }
                }
                val intent = Intent(context, InstallationReceiver::class.java).apply {
                    action = context.packageName + ACTION_INSTALL
                    `package` = context.packageName
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, sessionId, intent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                session.commit(pendingIntent.intentSender)
            }
        } catch (e: Exception) {
            EventBus.getDefault().postSticky(Event())
            Utils.error("Install failed: ${e.message}", e)
        }
    }

    private fun writeFileToSession(session: PackageInstaller.Session, stream: InputStream, name: String, size: Long) {
        session.openWrite(name, 0, if (size > 0) size else -1).use { out ->
            stream.copyTo(out, bufferSize = 128 * 1024)
        }

    }

    private fun clearAbandonedSessions(installer: PackageInstaller) {
        installer.mySessions.forEach { sessionInfo ->
            if (sessionInfo.sessionId == currentSession.get()) return@forEach
            try {
                installer.abandonSession(sessionInfo.sessionId)
            } catch (_: Exception) {}
        }
    }

    fun isPending() = AppServices.packageManager.packageInstaller.mySessions.firstOrNull {
        currentSession.get() == it.sessionId
    }?.isActive == false

    @Suppress("unused")
    private fun installStandardApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.file.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
