package com.rakshaai.security.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.rakshaai.security.MainActivity
import com.rakshaai.security.R
import com.rakshaai.security.data.local.ScanDatabase
import com.rakshaai.security.data.model.ContentType
import com.rakshaai.security.data.model.RiskLevel
import com.rakshaai.security.data.preference.SecurityPreferencesRepository
import com.rakshaai.security.data.repository.ScanRepository
import com.rakshaai.security.domain.apk.ApkMonitoringManager
import com.rakshaai.security.domain.engine.LocalDetectionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class RakshaNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val duplicateCache = ConcurrentHashMap<String, Long>()

    companion object {
        const val CHANNEL_ID = "rakshaai_security_alerts"
        const val ALERT_NOTIFICATION_ID = 2026
        private const val DUPLICATE_WINDOW_MS = 20_000L // 20 seconds in-memory duplicate suppression
        private const val RECENT_DB_WINDOW_MS = 20_000L // 20 seconds Room database deduplication window

        fun requestRebindService(context: Context) {
            try {
                requestRebind(ComponentName(context, RakshaNotificationListenerService::class.java))
            } catch (_: Throwable) {
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // 0. Check user preference setting
        SecurityPreferencesRepository.initialize(applicationContext)
        val settings = SecurityPreferencesRepository.settingsState.value
        if (!settings.notificationMonitoringEnabled) {
            return
        }

        val packageName = sbn.packageName ?: return
        if (packageName == applicationContext.packageName) {
            // Ignore RakshaAI's own notifications to prevent infinite loops
            return
        }

        val notification = sbn.notification ?: return
        val defaultTitle = notification.extras?.getCharSequence("android.title")?.toString() ?: ""

        // Extract ONLY single individual message lines instead of concatenating group summaries
        val singleMessages = extractSingleMessages(notification, defaultTitle)
        if (singleMessages.isEmpty()) return

        for (singleMsg in singleMessages) {
            val cleanText = singleMsg.trim()
            if (cleanText.isEmpty() || isSummaryText(cleanText)) continue

            // 1. Unique text hash deduplication
            val notificationKey = "$packageName:${cleanText.lowercase().hashCode()}"
            val currentTime = System.currentTimeMillis()
            val lastSeen = duplicateCache[notificationKey] ?: 0L
            if (currentTime - lastSeen < DUPLICATE_WINDOW_MS) {
                continue
            }
            duplicateCache[notificationKey] = currentTime

            if (duplicateCache.size > 200) {
                val threshold = currentTime - DUPLICATE_WINDOW_MS
                duplicateCache.entries.removeIf { it.value < threshold }
            }

            scope.launch {
                try {
                    ScanRepository.initialize(applicationContext)

                    // Check if this notification is a browser/file download notification for an APK file
                    if (cleanText.contains(".apk", ignoreCase = true)) {
                        val apkName = extractApkFileName(cleanText)
                        if (apkName != null) {
                            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                            val apkFile = java.io.File(downloadsDir, apkName)
                            if (apkFile.exists() && apkFile.length() > 0) {
                                ApkMonitoringManager.scanApkUri(applicationContext, android.net.Uri.fromFile(apkFile), apkName)
                            }
                        }
                    }

                    // 2. Cross-restart Database deduplication check for this single message
                    val database = ScanDatabase.getDatabase(applicationContext)
                    val recentCount = database.scanDao().countRecentScansWithContent(cleanText, currentTime - RECENT_DB_WINDOW_MS)
                    if (recentCount > 0) {
                        return@launch
                    }

                    val result = LocalDetectionEngine.analyzeContent(ContentType.MESSAGE, cleanText, null)
                    val notificationResult = result.copy(isNotification = true)

                    // Save single message scan to Room database history
                    ScanRepository.saveScan(notificationResult)

                    if (settings.securityAlertsEnabled && (
                                notificationResult.riskLevel == RiskLevel.HIGH_RISK ||
                                notificationResult.riskLevel == RiskLevel.DANGEROUS ||
                                notificationResult.riskLevel == RiskLevel.SUSPICIOUS
                            )) {
                        showSecurityAlert(applicationContext, notificationResult, sbn.id)
                    }
                } catch (_: Throwable) {
                    // Fail gracefully
                }
            }
        }
    }

    private fun extractSingleMessages(notification: Notification, defaultTitle: String): List<String> {
        val extras = notification.extras ?: return emptyList()
        val extractedMessages = mutableListOf<String>()

        // 1. Check NotificationCompat.MessagingStyle (WhatsApp, Signal, Telegram, Messages)
        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
            val latestMsg = messagingStyle.messages.lastOrNull()
            if (latestMsg != null && !latestMsg.text.isNullOrBlank()) {
                @Suppress("DEPRECATION")
                val sender = latestMsg.person?.name?.toString()
                    ?: latestMsg.sender?.toString()
                    ?: messagingStyle.conversationTitle?.toString()
                    ?: defaultTitle
                val cleanSender = if (sender.isBlank() || sender == defaultTitle || isSummaryText(sender)) "" else "$sender: "
                val textStr = latestMsg.text.toString().trim()
                if (textStr.isNotEmpty() && !isSummaryText(textStr)) {
                    extractedMessages.add("$cleanSender$textStr".trim())
                    return extractedMessages
                }
            }
        }

        // 2. Check InboxStyle (textLines array)
        val textLines = extras.getCharSequenceArray("android.textLines")
        if (textLines != null && textLines.isNotEmpty()) {
            val lastLine = textLines.lastOrNull()?.toString()?.trim() ?: ""
            if (lastLine.isNotEmpty() && !isSummaryText(lastLine)) {
                val titleStr = extras.getCharSequence("android.title")?.toString()?.trim() ?: ""
                val cleanTitle = if (titleStr.isNotBlank() && !isSummaryText(titleStr)) "$titleStr: " else ""
                extractedMessages.add("$cleanTitle$lastLine".trim())
                return extractedMessages
            }
        }

        // 3. Fallback: Standard Single Notification
        val titleStr = extras.getCharSequence("android.title")?.toString()?.trim() ?: ""
        val textStr = extras.getCharSequence("android.text")?.toString()?.trim() ?: ""
        val bigTextStr = extras.getCharSequence("android.bigText")?.toString()?.trim() ?: ""

        val bestText = when {
            bigTextStr.isNotBlank() && !isSummaryText(bigTextStr) -> bigTextStr
            textStr.isNotBlank() && !isSummaryText(textStr) -> textStr
            else -> ""
        }

        if (bestText.isNotBlank()) {
            val cleanTitle = if (titleStr.isNotBlank() && !isSummaryText(titleStr)) "$titleStr: " else ""
            extractedMessages.add("$cleanTitle$bestText".trim())
        }

        return extractedMessages
    }

    private fun isSummaryText(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("messages from") ||
                lower.contains("new messages") ||
                lower.contains("unread messages") ||
                lower.matches(Regex(".*\\d+\\s+messages.*")) ||
                lower.matches(Regex(".*\\d+\\s+chats.*"))
    }

    private fun extractApkFileName(text: String): String? {
        val regex = Regex("([a-zA-Z0-9_\\-\\.]+\\.apk)", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        return match?.value
    }

    private fun showSecurityAlert(context: Context, scanResult: com.rakshaai.security.data.model.ScanResult, sbnId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RakshaAI Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time security warnings for suspicious and scam notifications."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("scan_id", scanResult.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            sbnId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alertTitle = when (scanResult.riskLevel) {
            RiskLevel.DANGEROUS -> "⚠ Dangerous Threat Detected"
            RiskLevel.HIGH_RISK -> "Potential Scam Notification"
            RiskLevel.SUSPICIOUS -> "Suspicious Notification Detected"
            RiskLevel.SAFE -> "Security Alert"
        }

        val shortReason = when {
            scanResult.warningSigns.isNotEmpty() -> scanResult.warningSigns.first()
            scanResult.signals.isNotEmpty() -> scanResult.signals.first().title
            else -> "Possible financial or phishing scam detected."
        }

        val bodyText = "${scanResult.riskLevel.label} (Score: ${scanResult.riskScore}/100) • $shortReason"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🛡 RakshaAI: $alertTitle")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$alertTitle\n\nRisk Level: ${scanResult.riskLevel.label} (Score: ${scanResult.riskScore}/100)\n\nReason: $shortReason\n\nNotification Text: \"${scanResult.rawContent}\"\n\nTap to open RakshaAI and view complete analysis."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "View Detection", pendingIntent)
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID + (sbnId.hashCode() % 10000), notification)
    }
}
