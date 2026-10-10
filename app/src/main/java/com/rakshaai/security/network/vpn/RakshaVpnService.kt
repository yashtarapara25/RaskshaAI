package com.rakshaai.security.network.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.rakshaai.security.MainActivity
import com.rakshaai.security.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream

class RakshaVpnService : VpnService() {

    companion object {
        const val ACTION_START_VPN = "com.rakshaai.security.START_VPN"
        const val ACTION_STOP_VPN = "com.rakshaai.security.STOP_VPN"
        private const val NOTIFICATION_ID = 2025
        private const val SERVICE_CHANNEL_ID = "rakshaai_vpn_service_channel"
        private const val ALERTS_CHANNEL_ID = "rakshaai_network_alerts"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_VPN
        if (action == ACTION_STOP_VPN) {
            stopVpn()
            return START_NOT_STICKY
        }

        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        try {
            promoteToForeground()

            val builder = Builder()
                .setSession("RakshaAI Local Security Monitor")
                .addAddress("10.10.0.1", 24)
                .addRoute("10.10.0.0", 24)
                .addDnsServer("8.8.8.8")

            vpnInterface = builder.establish()
            VpnManager.updateVpnRunningState(true)

            // Trigger real-time pop-up notification when local VPN connects
            com.rakshaai.security.data.preference.SecurityPreferencesRepository.initialize(applicationContext)
            val settings = com.rakshaai.security.data.preference.SecurityPreferencesRepository.settingsState.value
            if (settings.securityAlertsEnabled) {
                showNetworkConnectedAlert(applicationContext)
            }

            vpnJob = scope.launch {
                val pfd = vpnInterface ?: return@launch
                val inputStream = FileInputStream(pfd.fileDescriptor)
                val buffer = ByteArray(32767)
                var lastAlertTime = 0L

                while (isActive) {
                    try {
                        val read = inputStream.read(buffer)
                        if (read <= 0) break

                        // Inspect IP packet headers for unencrypted port 80 (HTTP) traffic
                        if (read >= 20) {
                            val ipHeaderLength = (buffer[0].toInt() and 0x0F) * 4
                            if (read >= ipHeaderLength + 4) {
                                val destPort = ((buffer[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or (buffer[ipHeaderLength + 3].toInt() and 0xFF)
                                val now = System.currentTimeMillis()
                                if (destPort == 80 && now - lastAlertTime > 30_000L) {
                                    lastAlertTime = now
                                    if (settings.securityAlertsEnabled) {
                                        showUnencryptedNetworkAlert(applicationContext)
                                    }
                                }
                            }
                        }
                    } catch (_: Throwable) {
                        break
                    }
                }
            }
        } catch (_: Throwable) {
            stopVpn()
        }
    }

    private fun promoteToForeground() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "RakshaAI Network Protection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing local security VPN for network metadata monitoring."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🛡 RakshaAI Network Monitor Active")
            .setContentText("Local security VPN is observing connection metadata.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            if (type != 0) {
                startForeground(NOTIFICATION_ID, notification, type)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun showNetworkConnectedAlert(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERTS_CHANNEL_ID,
                "RakshaAI Network Protection Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time pop-up warnings for network protection events."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            20251,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ALERTS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚡ RakshaAI: Network Protection Connected")
            .setContentText("Local security firewall active (10.10.0.1). Inspecting connections.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("⚡ RakshaAI: Local Network Protection Connected\n\nLocal security firewall active (10.10.0.1). Inspecting background IP traffic and unencrypted connections on-device."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(202511, notification)
    }

    private fun showUnencryptedNetworkAlert(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERTS_CHANNEL_ID,
                "RakshaAI Network Protection Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time pop-up warnings for network protection events."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            8080,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ALERTS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚡ RakshaAI: Unencrypted Connection Detected")
            .setContentText("Background connection over plain HTTP (Port 80) detected.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("⚡ RakshaAI: Unencrypted Connection Detected\n\nAn app on your device made a plain HTTP background connection. Tap to review security settings."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(202580, notification)
    }

    private fun stopVpn() {
        vpnJob?.cancel()
        vpnJob = null

        try {
            vpnInterface?.close()
        } catch (_: Throwable) {
        }
        vpnInterface = null

        VpnManager.updateVpnRunningState(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
