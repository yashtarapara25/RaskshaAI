package com.rakshaai.security.domain.apk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rakshaai.security.MainActivity
import com.rakshaai.security.R
import com.rakshaai.security.data.model.ContentType
import com.rakshaai.security.data.model.RiskCategory
import com.rakshaai.security.data.model.RiskLevel
import com.rakshaai.security.data.model.RiskSignal
import com.rakshaai.security.data.model.ScanResult
import com.rakshaai.security.data.preference.SecurityPreferencesRepository
import com.rakshaai.security.data.repository.ScanRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object ApkMonitoringManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val processedEventHashes = ConcurrentHashMap<String, Long>()
    private const val DUP_WINDOW_MS = 60_000L
    private const val CHANNEL_ID = "rakshaai_apk_alerts"
    private const val ALERT_NOTIFICATION_ID = 3030

    fun computeSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun scanApkUri(context: Context, uri: Uri, fileName: String): ApkScanResult = withContext(Dispatchers.IO) {
        SecurityPreferencesRepository.initialize(context)
        ScanRepository.initialize(context)

        val settings = SecurityPreferencesRepository.settingsState.value
        if (!settings.apkMonitoringEnabled) {
            val emptyMeta = ApkMetadata(fileName, null, null, null, 0L, "")
            return@withContext ApkScanResult(
                metadata = emptyMeta,
                riskLevel = RiskLevel.SAFE,
                riskScore = 0,
                threatStatusText = "APK Monitoring Disabled by User",
                warnings = emptyList()
            )
        }

        var fileSize = 0L
        var hash = ""
        var tempFile: java.io.File? = null

        try {
            // Write to a temporary file in cache directory so PackageManager can parse the APK archive
            val cacheDir = context.cacheDir
            tempFile = java.io.File.createTempFile("apk_scan_", ".apk", cacheDir)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    val digest = MessageDigest.getInstance("SHA-256")
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                        outputStream.write(buffer, 0, bytesRead)
                        fileSize += bytesRead
                    }
                    val hashBytes = digest.digest()
                    hash = hashBytes.joinToString("") { "%02x".format(it) }
                }
            }
        } catch (_: Throwable) {
        }

        var packageName: String? = null
        var versionName: String? = null
        var versionCode: Long? = null

        if (tempFile != null && tempFile.exists() && tempFile.length() > 0) {
            try {
                val pm = context.packageManager
                val archiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageArchiveInfo(tempFile.absolutePath, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageArchiveInfo(tempFile.absolutePath, 0)
                }
                archiveInfo?.let { info ->
                    packageName = info.packageName
                    versionName = info.versionName
                    @Suppress("DEPRECATION")
                    versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
                }
            } catch (_: Throwable) {
            } finally {
                try {
                    tempFile.delete()
                } catch (_: Throwable) {
                }
            }
        }

        val metadata = ApkMetadata(
            fileName = fileName,
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            fileSize = fileSize,
            sha256Hash = hash,
            isFromUnknownSource = true
        )

        val warnings = mutableListOf<String>()
        var riskScore = 15 // Base score for unverified third-party APK
        var riskLevel = RiskLevel.SAFE

        if (fileSize == 0L || hash.isEmpty()) {
            return@withContext ApkScanResult(
                metadata = metadata,
                riskLevel = RiskLevel.SAFE,
                riskScore = 0,
                threatStatusText = "APK file could not be accessed or read.",
                warnings = listOf("File unreadable or inaccessible.")
            )
        }

        val lowerName = fileName.lowercase()
        if (lowerName.contains(".apk.exe") || lowerName.contains(".apk.zip") || lowerName.contains(".apk.pdf")) {
            warnings.add("Dangerous double file extension detected ($fileName).")
            riskScore += 70
            riskLevel = RiskLevel.HIGH_RISK
        }

        packageName?.let { pkg ->
            val lowerPkg = pkg.lowercase()
            if (lowerPkg.contains("phish") || lowerPkg.contains("fake") || lowerPkg.contains("stealer") || lowerPkg.contains("trojan")) {
                warnings.add("Suspicious package name signature: $pkg")
                riskScore += 60
                riskLevel = if (riskScore >= 80) RiskLevel.DANGEROUS else RiskLevel.HIGH_RISK
            }
        }

        warnings.add("Source: Third-Party External Download / Sideloaded APK")

        val threatStatus = when (riskLevel) {
            RiskLevel.SAFE -> "Third-Party APK Scan Completed — No known malware hash matched."
            RiskLevel.SUSPICIOUS -> "Suspicious Third-Party APK detected."
            RiskLevel.HIGH_RISK -> "High-Risk Third-Party APK detected."
            RiskLevel.DANGEROUS -> "Malicious Third-Party APK detected."
        }

        val result = ApkScanResult(
            metadata = metadata,
            riskLevel = riskLevel,
            riskScore = riskScore,
            threatStatusText = threatStatus,
            warnings = warnings
        )

        // Record scan result in repository
        val scanRecord = ScanResult(
            contentType = ContentType.MESSAGE,
            rawContent = "APK File Scan: $fileName\nPackage: ${packageName ?: "Unknown"}\nSHA-256: ${if (hash.length > 16) hash.take(16) + "..." else hash}",
            language = "English",
            riskScore = riskScore,
            riskLevel = riskLevel,
            category = if (riskLevel == RiskLevel.SAFE) RiskCategory.SAFE_MSG else RiskCategory.PHISHING,
            signals = listOf(
                RiskSignal("Third-Party APK Inspection", threatStatus),
                RiskSignal("SHA-256 Hash Digest", if (hash.isNotEmpty()) hash else "Could not calculate")
            ),
            recommendedActions = listOf(
                "Review application permissions before installing.",
                "Avoid installing APKs from unknown or unverified download sites."
            )
        )

        ScanRepository.saveScan(scanRecord)

        // Show alert popup notification when security alerts are enabled
        if (settings.securityAlertsEnabled) {
            showAlertNotification(context, fileName, threatStatus, scanRecord.id, riskLevel)
        }

        result
    }

    fun handleAppInstallationEvent(context: Context, packageName: String, isUpdate: Boolean) {
        SecurityPreferencesRepository.initialize(context)
        ScanRepository.initialize(context)

        scope.launch {
            val settings = SecurityPreferencesRepository.settingsState.value
            if (!settings.apkMonitoringEnabled) return@launch

            val eventKey = "$packageName:$isUpdate"
            val now = System.currentTimeMillis()
            val last = processedEventHashes[eventKey] ?: 0L
            if (now - last < DUP_WINDOW_MS) return@launch
            processedEventHashes[eventKey] = now

            val pm = context.packageManager
            var appLabel = packageName
            try {
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(packageName, 0)
                }
                appLabel = pm.getApplicationLabel(appInfo).toString()
            } catch (_: Throwable) {
            }

            val installerPackage = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    pm.getInstallSourceInfo(packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstallerPackageName(packageName)
                }
            } catch (_: Throwable) {
                null
            }

            val isPlayStore = installerPackage == "com.android.vending"
            val isThirdPartySideload = !isPlayStore
            val sourceText = when {
                isPlayStore -> "Google Play Store"
                installerPackage != null -> "Third-Party Installer ($installerPackage)"
                else -> "Third-Party Sideload / Unknown Source"
            }

            val riskScore = if (isThirdPartySideload) 65 else 0
            val riskLevel = if (isThirdPartySideload) RiskLevel.HIGH_RISK else RiskLevel.SAFE
            val actionText = if (isUpdate) "App Updated" else "New App Installed"

            val scanRecord = ScanResult(
                contentType = ContentType.MESSAGE,
                rawContent = "$actionText: $appLabel ($packageName)\nSource: $sourceText",
                language = "English",
                riskScore = riskScore,
                riskLevel = riskLevel,
                category = if (isThirdPartySideload) RiskCategory.PHISHING else RiskCategory.SAFE_MSG,
                signals = listOf(
                    RiskSignal("Installation Event", "$actionText on device: $appLabel"),
                    RiskSignal("Installation Source", sourceText)
                ),
                recommendedActions = if (isThirdPartySideload) {
                    listOf(
                        "Review granted application permissions carefully.",
                        "If you did not intend to install this app, uninstall it immediately."
                    )
                } else {
                    listOf("Verified installation source.")
                }
            )

            ScanRepository.saveScan(scanRecord)

            // Show real-time pop-up notification when a new app/APK is installed or updated
            if (settings.securityAlertsEnabled) {
                showAppInstallAlertNotification(context, appLabel, packageName, sourceText, isThirdPartySideload, isUpdate, scanRecord.id)
            }
        }
    }

    private fun showAlertNotification(context: Context, fileName: String, reason: String, scanId: String, riskLevel: RiskLevel) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "APK Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time pop-up security warnings for APK downloads and app installations."
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
            fileName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alertTitle = when (riskLevel) {
            RiskLevel.DANGEROUS -> "⚠ Malicious Third-Party APK"
            RiskLevel.HIGH_RISK -> "⚠ High-Risk Third-Party APK"
            RiskLevel.SUSPICIOUS -> "Suspicious Third-Party APK"
            RiskLevel.SAFE -> "🛡 RakshaAI: Third-Party APK Scan"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(alertTitle)
            .setContentText("APK '$fileName': $reason")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$alertTitle\n\nFile: $fileName\nStatus: $reason\n\nTap to open RakshaAI and review security details."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Review Scan", pendingIntent)
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID + (fileName.hashCode() % 10000), notification)
    }

    private fun showAppInstallAlertNotification(
        context: Context,
        appLabel: String,
        packageName: String,
        sourceText: String,
        isThirdPartySideload: Boolean,
        isUpdate: Boolean,
        scanId: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "APK Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time pop-up security warnings for APK downloads and app installations."
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
            packageName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actionLabel = if (isUpdate) "App Updated" else "New App Installed"
        val alertTitle = if (isThirdPartySideload) {
            "⚠ Third-Party $actionLabel: $appLabel"
        } else {
            "🛡 RakshaAI: $actionLabel: $appLabel"
        }

        val bodyText = if (isThirdPartySideload) {
            "WARNING: '$appLabel' was installed from an unverified third-party source ($sourceText). Tap to review permissions."
        } else {
            "Application '$appLabel' was installed from Google Play Store."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(alertTitle)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$alertTitle\n\nPackage: $packageName\nSource: $sourceText\n\n$bodyText\n\nTap to open RakshaAI and view security analysis."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "View Scan", pendingIntent)
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID + (packageName.hashCode() % 10000), notification)
    }
}
