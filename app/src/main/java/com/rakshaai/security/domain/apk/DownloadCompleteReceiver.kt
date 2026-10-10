package com.rakshaai.security.domain.apk

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DownloadCompleteReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) return

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
        val query = DownloadManager.Query().setFilterById(downloadId)

        scope.launch {
            try {
                downloadManager.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                        val mimeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)

                        val uriString = if (uriIndex != -1) cursor.getString(uriIndex) else null
                        val title = if (titleIndex != -1) cursor.getString(titleIndex) else "download.apk"
                        val mimeType = if (mimeIndex != -1) cursor.getString(mimeIndex) else ""

                        if (title.endsWith(".apk", ignoreCase = true) || mimeType == "application/vnd.android.package-archive") {
                            uriString?.let { uStr ->
                                ApkMonitoringManager.scanApkUri(context, Uri.parse(uStr), title)
                            }
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }
}
