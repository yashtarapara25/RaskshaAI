package com.rakshaai.security.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import com.rakshaai.security.data.repository.ScanRepository
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
        private const val DUPLICATE_WINDOW_MS = 60_000L // 60 seconds in-memory update suppression
        private const val RECENT_DB_WINDOW_MS = 300_000L // 5 minutes Room database deduplication window
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (packageName == applicationContext.packageName) {
            // Ignore RakshaAI's own notifications to prevent infinite loops
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        
        val textLines = extras.getCharSequenceArray("android.textLines")?.let { lines ->
            lines.joinToString(" ") { it?.toString() ?: "" }
        } ?: ""

        val combinedText = listOf(title, text, bigText, textLines)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()

        if (combinedText.isEmpty()) return

        // 1. In-memory notification ID update suppression
        val notificationKey = "$packageName:${sbn.id}"
        val currentTime = System.currentTimeMillis()
        val lastSeen = duplicateCache[notificationKey] ?: 0L
        if (currentTime - lastSeen < DUPLICATE_WINDOW_MS) {
            return
        }
        duplicateCache[notificationKey] = currentTime

        if (duplicateCache.size > 200) {
            val threshold = currentTime - DUPLICATE_WINDOW_MS
            duplicateCache.entries.removeIf { it.value < threshold }
        }

        scope.launch {
            try {
                ScanRepository.initialize(applicationContext)

                // 2. Cross-restart Database deduplication check
                val database = ScanDatabase.getDatabase(applicationContext)
                val recentCount = database.scanDao().countRecentScansWithContent(combinedText, currentTime - RECENT_DB_WINDOW_MS)
                if (recentCount > 0) {
                    return@launch
                }

                val result = LocalDetectionEngine.analyzeContent(ContentType.MESSAGE, combinedText, null)
                val notificationResult = result.copy(isNotification = true)
                
                // Save scan to Room database history
                ScanRepository.saveScan(notificationResult)

                if (notificationResult.riskLevel == RiskLevel.HIGH_RISK || notificationResult.riskLevel == RiskLevel.DANGEROUS) {
                    showSecurityAlert(applicationContext, notificationResult, sbn.id)
                }
            } catch (_: Throwable) {
                // Fail gracefully
            }
        }
    }

    private fun showSecurityAlert(context: Context, scanResult: com.rakshaai.security.data.model.ScanResult, sbnId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RakshaAI Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time security warnings for high-risk notifications."
                enableVibration(true)
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

        val alertTitle = if (scanResult.riskLevel == RiskLevel.DANGEROUS) {
            "⚠ Dangerous Threat Detected"
        } else {
            "Potential Scam Detected"
        }

        val shortReason = when {
            scanResult.warningSigns.isNotEmpty() -> scanResult.warningSigns.first()
            scanResult.signals.isNotEmpty() -> scanResult.signals.first().title
            else -> "Possible financial or phishing scam detected."
        }

        val bodyText = "${scanResult.riskLevel.label} (Score: ${scanResult.riskScore}) • $shortReason"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🛡 RakshaAI: $alertTitle")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$alertTitle\n\nRisk: ${scanResult.riskLevel.label} (Score: ${scanResult.riskScore})\nReason: $shortReason\n\nTap to view details."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "View Detection", pendingIntent)
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID + (sbnId % 10000), notification)
    }
}
