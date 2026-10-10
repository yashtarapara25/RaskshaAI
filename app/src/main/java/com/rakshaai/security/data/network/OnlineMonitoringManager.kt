package com.rakshaai.security.data.network

import android.content.Context
import android.net.VpnService
import android.os.Build
import androidx.core.content.ContextCompat
import com.rakshaai.security.data.preference.FeatureState
import com.rakshaai.security.data.preference.ProtectionFeatureStatus
import com.rakshaai.security.data.preference.SecurityPreferences
import com.rakshaai.security.data.preference.SecurityPreferencesRepository
import com.rakshaai.security.domain.url.ThreatIntelligenceStore
import com.rakshaai.security.network.vpn.VpnManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class OverallProtectionStatus(
    val title: String,
    val subtitle: String,
    val isOnlineProtected: Boolean,
    val isFullyProtected: Boolean
)

object OnlineMonitoringManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isInitialized = false

    private val _protectionStatus = MutableStateFlow(
        OverallProtectionStatus(
            title = "Offline Protection Active",
            subtitle = "Local ML model and threat rules are active",
            isOnlineProtected = false,
            isFullyProtected = false
        )
    )
    val protectionStatus: StateFlow<OverallProtectionStatus> = _protectionStatus.asStateFlow()

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        SecurityPreferencesRepository.initialize(context)
        NetworkConnectivityObserver.startObserving(context)
        com.rakshaai.security.domain.apk.DownloadDirectoryObserver.startObserving(context)

        // Preload local threat database
        ThreatIntelligenceStore.preloadAsync(context)

        scope.launch {
            combine(
                NetworkConnectivityObserver.isOnline,
                SecurityPreferencesRepository.settingsState,
                VpnManager.isVpnRunning
            ) { isOnline, settings, isVpnRunning ->
                Triple(isOnline, settings, isVpnRunning)
            }.collect { (isOnline, settings, _) ->
                updateStatus(context, isOnline, settings)
            }
        }
    }

    private fun updateStatus(
        context: Context,
        isOnline: Boolean,
        settings: SecurityPreferences
    ) {
        val websiteActive = settings.websiteMonitoringEnabled
        val apkActive = settings.apkMonitoringEnabled
        val notifActive = settings.notificationMonitoringEnabled && hasNotificationAccess(context)
        val networkActive = settings.networkMonitoringEnabled && VpnManager.isVpnRunning.value

        val title: String
        val subtitle: String
        val isOnlineProtected: Boolean
        val isFullyProtected: Boolean

        if (!isOnline) {
            title = "Offline Protection Active"
            subtitle = "Local ML model & threat rules active. Online checks paused."
            isOnlineProtected = false
            isFullyProtected = false
        } else if (websiteActive && apkActive && notifActive && networkActive) {
            title = "Fully Protected"
            subtitle = "All online, download, notification & network protection active."
            isOnlineProtected = true
            isFullyProtected = true
        } else if (websiteActive && apkActive) {
            title = "Online Protection Active"
            subtitle = "Real-time URL reputation & download security active."
            isOnlineProtected = true
            isFullyProtected = notifActive
        } else if (websiteActive || apkActive || notifActive || networkActive) {
            title = "Partial Protection Active"
            subtitle = "Some monitoring features are currently active."
            isOnlineProtected = true
            isFullyProtected = false
        } else {
            title = "Local Protection Only"
            subtitle = "Online monitoring disabled by user preferences."
            isOnlineProtected = false
            isFullyProtected = false
        }

        _protectionStatus.value = OverallProtectionStatus(
            title = title,
            subtitle = subtitle,
            isOnlineProtected = isOnlineProtected,
            isFullyProtected = isFullyProtected
        )
    }

    fun hasNotificationAccess(context: Context): Boolean {
        return try {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            enabledListeners != null && enabledListeners.contains(context.packageName)
        } catch (_: Throwable) {
            false
        }
    }

    fun hasPostNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun hasVpnConsent(context: Context): Boolean {
        return VpnService.prepare(context) == null
    }

    fun hasAccessibilityPermission(context: Context): Boolean {
        val serviceName = "${context.packageName}/${com.rakshaai.security.service.RakshaAccessibilityService::class.java.name}"
        return try {
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            enabledServices != null && enabledServices.contains(serviceName)
        } catch (_: Throwable) {
            false
        }
    }

    fun getWebsiteFeatureState(isOnline: Boolean, isEnabled: Boolean): FeatureState {
        if (!isEnabled) {
            return FeatureState(
                isEnabled = false,
                status = ProtectionFeatureStatus.DISABLED,
                statusMessage = "Disabled by user"
            )
        }
        if (!isOnline) {
            return FeatureState(
                isEnabled = true,
                status = ProtectionFeatureStatus.WAITING_FOR_INTERNET,
                statusMessage = "Waiting for internet connection"
            )
        }
        return FeatureState(
            isEnabled = true,
            isRuntimeActive = true,
            status = ProtectionFeatureStatus.ACTIVE,
            statusMessage = "Active - scanning URLs against local & online threat feeds"
        )
    }

    fun getApkFeatureState(isEnabled: Boolean): FeatureState {
        if (!isEnabled) {
            return FeatureState(
                isEnabled = false,
                status = ProtectionFeatureStatus.DISABLED,
                statusMessage = "Disabled by user"
            )
        }
        return FeatureState(
            isEnabled = true,
            isRuntimeActive = true,
            status = ProtectionFeatureStatus.ACTIVE,
            statusMessage = "Active - monitoring download events & app installations"
        )
    }

    fun getNotificationFeatureState(context: Context, isEnabled: Boolean): FeatureState {
        if (!isEnabled) {
            return FeatureState(
                isEnabled = false,
                status = ProtectionFeatureStatus.DISABLED,
                statusMessage = "Disabled by user"
            )
        }
        val hasPerm = hasNotificationAccess(context)
        if (!hasPerm) {
            return FeatureState(
                isEnabled = true,
                hasPermission = false,
                status = ProtectionFeatureStatus.PERMISSION_REQUIRED,
                statusMessage = "Notification Access permission required"
            )
        }
        return FeatureState(
            isEnabled = true,
            hasPermission = true,
            isRuntimeActive = true,
            status = ProtectionFeatureStatus.ACTIVE,
            statusMessage = "Active - inspecting incoming notifications for scam indicators"
        )
    }

    fun getNetworkFeatureState(context: Context, isEnabled: Boolean): FeatureState {
        if (!isEnabled) {
            return FeatureState(
                isEnabled = false,
                status = ProtectionFeatureStatus.DISABLED,
                statusMessage = "Disabled by user"
            )
        }
        val hasConsent = hasVpnConsent(context)
        if (!hasConsent) {
            return FeatureState(
                isEnabled = true,
                hasPermission = false,
                status = ProtectionFeatureStatus.PERMISSION_REQUIRED,
                statusMessage = "Local VPN authorization required"
            )
        }
        var isRunning = VpnManager.isVpnRunning.value
        if (!isRunning) {
            try {
                VpnManager.startVpnService(context)
                isRunning = VpnManager.isVpnRunning.value
            } catch (_: Throwable) {
            }
        }
        return if (isRunning) {
            FeatureState(
                isEnabled = true,
                hasPermission = true,
                isRuntimeActive = true,
                status = ProtectionFeatureStatus.ACTIVE,
                statusMessage = "Active - local VPN active for connection metadata analysis"
            )
        } else {
            FeatureState(
                isEnabled = true,
                hasPermission = true,
                isRuntimeActive = false,
                status = ProtectionFeatureStatus.PERMISSION_REQUIRED,
                statusMessage = "Tap Authorize Local VPN to activate"
            )
        }
    }

    fun getThreatUpdatesFeatureState(isOnline: Boolean, isEnabled: Boolean): FeatureState {
        if (!isEnabled) {
            return FeatureState(
                isEnabled = false,
                status = ProtectionFeatureStatus.DISABLED,
                statusMessage = "Disabled by user"
            )
        }
        if (!isOnline) {
            return FeatureState(
                isEnabled = true,
                status = ProtectionFeatureStatus.WAITING_FOR_INTERNET,
                statusMessage = "Waiting for internet connection to refresh threat database"
            )
        }
        return FeatureState(
            isEnabled = true,
            isRuntimeActive = true,
            status = ProtectionFeatureStatus.ACTIVE,
            statusMessage = "Active - scheduled periodic threat database updates"
        )
    }

    fun getSecurityAlertsFeatureState(context: Context, isEnabled: Boolean): FeatureState {
        if (!isEnabled) {
            return FeatureState(
                isEnabled = false,
                status = ProtectionFeatureStatus.DISABLED,
                statusMessage = "Disabled by user"
            )
        }
        val hasPerm = hasPostNotificationPermission(context)
        if (!hasPerm) {
            return FeatureState(
                isEnabled = true,
                hasPermission = false,
                status = ProtectionFeatureStatus.PERMISSION_REQUIRED,
                statusMessage = "Notification permission required for system alerts"
            )
        }
        return FeatureState(
            isEnabled = true,
            hasPermission = true,
            isRuntimeActive = true,
            status = ProtectionFeatureStatus.ACTIVE,
            statusMessage = "Active - real-time alerts enabled for suspicious findings"
        )
    }
}
