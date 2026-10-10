package com.rakshaai.security.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.rakshaai.security.MainActivity
import com.rakshaai.security.R
import com.rakshaai.security.data.model.ContentType
import com.rakshaai.security.data.model.RiskCategory
import com.rakshaai.security.data.model.RiskLevel
import com.rakshaai.security.data.model.RiskSignal
import com.rakshaai.security.data.model.ScanResult
import com.rakshaai.security.data.network.NetworkConnectivityObserver
import com.rakshaai.security.data.preference.SecurityPreferencesRepository
import com.rakshaai.security.data.repository.ScanRepository
import com.rakshaai.security.domain.url.UrlAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class RakshaAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val scannedUrlCache = ConcurrentHashMap<String, Long>()

    companion object {
        const val CHANNEL_ID = "rakshaai_browser_alerts"
        const val ALERT_NOTIFICATION_ID = 4040
        private const val DUP_WINDOW_MS = 15_000L

        private val ALLOWED_BROWSERS = setOf(
            "com.android.chrome",
            "com.google.android.googlequicksearchbox",
            "com.vivo.browser",
            "com.heytap.browser",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
            "com.opera.browser",
            "com.brave.browser"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Fast browser package check — ignore System UI, Notification shade, and non-browser apps instantly
        val pkgName = event.packageName?.toString() ?: return
        if (!ALLOWED_BROWSERS.contains(pkgName)) return

        SecurityPreferencesRepository.initialize(applicationContext)
        val settings = SecurityPreferencesRepository.settingsState.value
        if (!settings.websiteMonitoringEnabled) return

        val now = System.currentTimeMillis()

        scope.launch {
            try {
                val detectedUrls = mutableSetOf<String>()

                // 1. Extract from event text
                for (txt in event.text) {
                    val candidate = txt.toString().trim()
                    if (isCandidateUrl(candidate)) {
                        detectedUrls.add(cleanUrl(candidate))
                    }
                }
                val desc = event.contentDescription?.toString()
                if (!desc.isNullOrBlank() && isCandidateUrl(desc)) {
                    detectedUrls.add(cleanUrl(desc))
                }

                // 2. Extract from active browser window node tree
                val sourceNode = event.source ?: rootInActiveWindow
                if (sourceNode != null) {
                    extractUrlsFromNode(sourceNode, detectedUrls, maxDepth = 0)
                }

                if (detectedUrls.isEmpty()) return@launch

                val isOnline = NetworkConnectivityObserver.isOnline.value

                for (url in detectedUrls) {
                    val urlKey = url.lowercase().trim()
                    val lastSeen = scannedUrlCache[urlKey] ?: 0L
                    if (now - lastSeen < DUP_WINDOW_MS) continue
                    scannedUrlCache[urlKey] = now

                    ScanRepository.initialize(applicationContext)
                    val urlResult = UrlAnalyzer.analyzeAsync(url, isOnline)

                    if (urlResult.riskLevel != RiskLevel.SAFE || urlResult.score >= 15) {
                        val scanRecord = ScanResult(
                            contentType = ContentType.URL,
                            rawContent = url,
                            language = "English",
                            riskScore = urlResult.score,
                            riskLevel = urlResult.riskLevel,
                            category = RiskCategory.PHISHING,
                            signals = urlResult.signals.map { RiskSignal("Real-Time Browser Monitor", it) },
                            warningSigns = urlResult.warningTags,
                            recommendedActions = listOf(
                                "Do NOT enter passwords, credit cards, or banking PINs on this site.",
                                "Close this browser tab immediately."
                            )
                        )

                        // Save scan to Room database history
                        ScanRepository.saveScan(scanRecord)

                        if (settings.securityAlertsEnabled) {
                            showUnsafeBrowserAlert(applicationContext, url, urlResult.score, urlResult.riskLevel, scanRecord.id)
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun extractUrlsFromNode(node: AccessibilityNodeInfo?, outputUrls: MutableSet<String>, maxDepth: Int) {
        if (node == null || maxDepth > 8) return

        val text = node.text?.toString() ?: node.contentDescription?.toString()
        if (!text.isNullOrBlank()) {
            val candidate = text.trim()
            if (isCandidateUrl(candidate)) {
                outputUrls.add(cleanUrl(candidate))
            }
        }

        val childCount = node.childCount.coerceAtMost(12)
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                extractUrlsFromNode(child, outputUrls, maxDepth + 1)
            }
        }
    }

    private fun isCandidateUrl(text: String): Boolean {
        val lower = text.lowercase().trim()
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("www.")) return true
        if (lower.contains(" ") || lower.length < 4) return false

        if (lower.contains("@") || lower.contains(".xyz") || lower.contains(".top") || lower.contains(".online") || lower.contains(".tech") || lower.contains(".site") || lower.contains(".website") || lower.contains(".fun") || lower.contains(".click") || lower.contains(".link") || lower.contains(".phish") || lower.contains(".app") || lower.contains(".club") || lower.contains(".info") || lower.contains(".work") || lower.contains(".icu") || lower.contains(".loan") || lower.contains(".zip") || lower.contains(".exe") || lower.contains(".invalid") || lower.contains(".shop") || lower.contains(".store")) return true

        return lower.matches(Regex("^[a-zA-Z0-9\\-_@]+\\.[a-zA-Z]{2,}(/.*)?$"))
    }

    private fun cleanUrl(text: String): String {
        var clean = text.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        return clean
    }

    override fun onInterrupt() {}

    private fun showUnsafeBrowserAlert(
        context: Context,
        url: String,
        riskScore: Int,
        riskLevel: RiskLevel,
        scanId: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Real-Time Browser Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time pop-up warnings for suspicious websites visited in Chrome, Vivo Browser, or Google."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("scan_id", scanId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            url.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alertTitle = when (riskLevel) {
            RiskLevel.DANGEROUS -> "🔴 DANGEROUS WEBSITE IN BROWSER"
            RiskLevel.HIGH_RISK -> "⚠ HIGH-RISK WEBSITE WARNING"
            RiskLevel.SUSPICIOUS -> "⚡ SUSPICIOUS WEBSITE DETECTED"
            RiskLevel.SAFE -> "Website Security Check"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(alertTitle)
            .setContentText("URL '$url' (Score: $riskScore/100) is unsafe!")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$alertTitle\n\nURL: $url\nRisk Score: $riskScore / 100 (${riskLevel.label})\n\n⛔ ADVISORY: Do NOT enter passwords, banking details, or personal information on this site.\n\nTap to open RakshaAI and view full analysis."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Review Security Scan", pendingIntent)
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID + (url.hashCode() % 10000), notification)
    }
}
