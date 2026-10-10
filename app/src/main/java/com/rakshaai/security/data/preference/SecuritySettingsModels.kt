package com.rakshaai.security.data.preference

enum class ProtectionFeatureStatus(val label: String) {
    ACTIVE("Active"),
    WAITING_FOR_INTERNET("Waiting for Internet"),
    PERMISSION_REQUIRED("Permission Required"),
    DISABLED("Disabled by User"),
    NOT_CONFIGURED("Not Configured"),
    FAILED_TO_START("Failed to Start")
}

data class FeatureState(
    val isEnabled: Boolean,
    val hasPermission: Boolean = true,
    val isRuntimeActive: Boolean = false,
    val status: ProtectionFeatureStatus = ProtectionFeatureStatus.DISABLED,
    val lastActivityTime: Long = 0L,
    val statusMessage: String = ""
)

data class SecurityPreferences(
    val websiteMonitoringEnabled: Boolean = true,
    val apkMonitoringEnabled: Boolean = true,
    val notificationMonitoringEnabled: Boolean = false,
    val networkMonitoringEnabled: Boolean = false,
    val automaticThreatUpdatesEnabled: Boolean = true,
    val securityAlertsEnabled: Boolean = true
)
