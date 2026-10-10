package com.rakshaai.security.domain.apk

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object DownloadDirectoryObserver {

    private var fileObserver: FileObserver? = null
    private val processedFiles = ConcurrentHashMap<String, Long>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val DUP_WINDOW_MS = 15_000L

    fun startObserving(context: Context) {
        if (fileObserver != null) return

        val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsFolder.exists()) {
            try {
                downloadsFolder.mkdirs()
            } catch (_: Throwable) {
            }
        }

        val mask = FileObserver.CLOSE_WRITE or FileObserver.MOVED_TO or FileObserver.CREATE

        val observer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(downloadsFolder, mask) {
                override fun onEvent(event: Int, path: String?) {
                    handleFileEvent(context, downloadsFolder, path)
                }
            }
        } else {
            @Suppress("DEPRECATION")
            object : FileObserver(downloadsFolder.absolutePath, mask) {
                override fun onEvent(event: Int, path: String?) {
                    handleFileEvent(context, downloadsFolder, path)
                }
            }
        }

        try {
            observer.startWatching()
            fileObserver = observer
        } catch (_: Throwable) {
        }
    }

    fun stopObserving() {
        try {
            fileObserver?.stopWatching()
        } catch (_: Throwable) {
        }
        fileObserver = null
    }

    private fun handleFileEvent(context: Context, folder: File, fileName: String?) {
        if (fileName.isNullOrBlank()) return
        if (!fileName.endsWith(".apk", ignoreCase = true)) return
        if (fileName.contains(".crdownload", ignoreCase = true) || fileName.contains(".tmp", ignoreCase = true)) return

        val now = System.currentTimeMillis()
        val lastProcessed = processedFiles[fileName] ?: 0L
        if (now - lastProcessed < DUP_WINDOW_MS) return
        processedFiles[fileName] = now

        scope.launch {
            // Short delay to ensure Chrome/Browser finished writing & renaming the file
            delay(1000)

            val apkFile = File(folder, fileName)
            if (apkFile.exists() && apkFile.length() > 0) {
                val fileUri = Uri.fromFile(apkFile)
                ApkMonitoringManager.scanApkUri(context.applicationContext, fileUri, fileName)
            }
        }
    }
}
