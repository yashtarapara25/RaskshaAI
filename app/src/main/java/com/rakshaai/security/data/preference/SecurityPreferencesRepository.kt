package com.rakshaai.security.data.preference

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object SecurityPreferencesRepository {

    private const val PREFS_NAME = "rakshaai_security_preferences"

    private const val KEY_WEBSITE_MONITORING = "website_monitoring_enabled"
    private const val KEY_APK_MONITORING = "apk_monitoring_enabled"
    private const val KEY_NOTIFICATION_MONITORING = "notification_monitoring_enabled"
    private const val KEY_NETWORK_MONITORING = "network_monitoring_enabled"
    private const val KEY_AUTOMATIC_THREAT_UPDATES = "automatic_threat_updates_enabled"
    private const val KEY_SECURITY_ALERTS = "security_alerts_enabled"

    private var sharedPreferences: SharedPreferences? = null
    private var appContext: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _settingsState = MutableStateFlow(SecurityPreferences())
    val settingsState: StateFlow<SecurityPreferences> = _settingsState.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        loadSettings()
    }

    fun initialize(context: Context) {
        val app = context.applicationContext
        appContext = app
        if (sharedPreferences == null) {
            val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPreferences = prefs
            prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
            loadSettings()
        }
    }

    private fun loadSettings() {
        val prefs = sharedPreferences ?: return
        val current = SecurityPreferences(
            websiteMonitoringEnabled = prefs.getBoolean(KEY_WEBSITE_MONITORING, true),
            apkMonitoringEnabled = prefs.getBoolean(KEY_APK_MONITORING, true),
            notificationMonitoringEnabled = prefs.getBoolean(KEY_NOTIFICATION_MONITORING, false),
            networkMonitoringEnabled = prefs.getBoolean(KEY_NETWORK_MONITORING, false),
            automaticThreatUpdatesEnabled = prefs.getBoolean(KEY_AUTOMATIC_THREAT_UPDATES, true),
            securityAlertsEnabled = prefs.getBoolean(KEY_SECURITY_ALERTS, true)
        )
        _settingsState.value = current
    }

    fun setWebsiteMonitoringEnabled(enabled: Boolean) {
        scope.launch {
            sharedPreferences?.edit()?.putBoolean(KEY_WEBSITE_MONITORING, enabled)?.apply()
            loadSettings()
        }
    }

    fun setApkMonitoringEnabled(enabled: Boolean) {
        scope.launch {
            sharedPreferences?.edit()?.putBoolean(KEY_APK_MONITORING, enabled)?.apply()
            loadSettings()
        }
    }

    fun setNotificationMonitoringEnabled(enabled: Boolean) {
        scope.launch {
            sharedPreferences?.edit()?.putBoolean(KEY_NOTIFICATION_MONITORING, enabled)?.apply()
            loadSettings()
            appContext?.let { ctx ->
                if (enabled) {
                    com.rakshaai.security.notification.RakshaNotificationListenerService.requestRebindService(ctx)
                }
            }
        }
    }

    fun setNetworkMonitoringEnabled(enabled: Boolean) {
        scope.launch {
            sharedPreferences?.edit()?.putBoolean(KEY_NETWORK_MONITORING, enabled)?.apply()
            loadSettings()
            appContext?.let { ctx ->
                if (!enabled) {
                    com.rakshaai.security.network.vpn.VpnManager.stopVpnService(ctx)
                }
            }
        }
    }

    fun setAutomaticThreatUpdatesEnabled(enabled: Boolean) {
        scope.launch {
            sharedPreferences?.edit()?.putBoolean(KEY_AUTOMATIC_THREAT_UPDATES, enabled)?.apply()
            loadSettings()
            appContext?.let { ctx ->
                if (enabled) {
                    com.rakshaai.security.domain.url.ThreatUpdateWorker.schedulePeriodicWork(ctx)
                } else {
                    com.rakshaai.security.domain.url.ThreatUpdateWorker.cancelWork(ctx)
                }
            }
        }
    }

    fun setSecurityAlertsEnabled(enabled: Boolean) {
        scope.launch {
            sharedPreferences?.edit()?.putBoolean(KEY_SECURITY_ALERTS, enabled)?.apply()
            loadSettings()
        }
    }
}
