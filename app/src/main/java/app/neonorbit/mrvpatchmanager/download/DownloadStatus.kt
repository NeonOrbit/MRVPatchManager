package app.neonorbit.mrvpatchmanager.download

import java.io.File

sealed class DownloadStatus {
    object DOWNLOADING : DownloadStatus()
    data class FETCHING(val server: String) : DownloadStatus()
    data class PROGRESS(val current: Long, val total: Long) : DownloadStatus()
    data class FAILED(val error: String) : DownloadStatus()
    data class FINISHED(val file: File) : DownloadStatus()
}
