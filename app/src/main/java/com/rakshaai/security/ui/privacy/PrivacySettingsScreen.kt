package com.rakshaai.security.ui.privacy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rakshaai.security.R
import com.rakshaai.security.data.network.NetworkConnectivityObserver
import com.rakshaai.security.data.network.OnlineMonitoringManager
import com.rakshaai.security.data.preference.FeatureState
import com.rakshaai.security.data.preference.LanguageManager
import com.rakshaai.security.data.preference.ProtectionFeatureStatus
import com.rakshaai.security.data.preference.SecurityPreferencesRepository
import com.rakshaai.security.domain.apk.ApkScanResult
import com.rakshaai.security.domain.apk.ApkMonitoringManager
import com.rakshaai.security.domain.url.ThreatIntelligenceStore
import com.rakshaai.security.network.vpn.VpnManager
import com.rakshaai.security.ui.components.OfflineStatusBadge
import com.rakshaai.security.ui.theme.BorderSubtle
import com.rakshaai.security.ui.theme.BrandTeal
import com.rakshaai.security.ui.theme.BrandTealLight
import com.rakshaai.security.ui.theme.SafeGreen
import com.rakshaai.security.ui.theme.SuspiciousAmber
import com.rakshaai.security.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun PrivacySettingsScreen(
    onLanguageChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val settings by SecurityPreferencesRepository.settingsState.collectAsState()
    val isOnline by NetworkConnectivityObserver.isOnline.collectAsState()
    val isVpnRunning by VpnManager.isVpnRunning.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var currentLang by remember { mutableStateOf(LanguageManager.getLanguage(context)) }
    var selectedApkResult by remember { mutableStateOf<ApkScanResult?>(null) }
    var showStatusCheckDialog by remember { mutableStateOf(false) }
    var isManualUpdating by remember { mutableStateOf(false) }
    var manualUpdateStatusText by remember { mutableStateOf<String?>(null) }

    // Lifecycle observer to refresh permission states on resume
    var refreshTrigger by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // SAF File Picker for manual APK scanning
    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            coroutineScope.launch {
                val fileName = getFileNameFromUri(context, fileUri) ?: "downloaded_app.apk"
                val result = ApkMonitoringManager.scanApkUri(context, fileUri, fileName)
                selectedApkResult = result
            }
        }
    }

    // VPN Authorization Launcher
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            VpnManager.startVpnService(context)
        }
    }

    // System Notification Permission Launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        refreshTrigger++
    }

    val langDisplay = when (currentLang) {
        "gu" -> "ગુજરાતી (Gujarati)"
        "hi" -> "हिन्दी (Hindi)"
        else -> "English"
    }

    // Language Selector Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_choose_language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LanguageOptionRow("en", "English", currentLang) {
                        currentLang = "en"
                        LanguageManager.setLanguage(context, "en")
                        showLanguageDialog = false
                        onLanguageChanged("en")
                    }
                    LanguageOptionRow("gu", "ગુજરાતી (Gujarati)", currentLang) {
                        currentLang = "gu"
                        LanguageManager.setLanguage(context, "gu")
                        showLanguageDialog = false
                        onLanguageChanged("gu")
                    }
                    LanguageOptionRow("hi", "हिन्दी (Hindi)", currentLang) {
                        currentLang = "hi"
                        LanguageManager.setLanguage(context, "hi")
                        showLanguageDialog = false
                        onLanguageChanged("hi")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(text = stringResource(R.string.btn_cancel), color = TextSecondary, fontSize = 14.sp)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // APK Scan Result Dialog
    selectedApkResult?.let { apkRes ->
        AlertDialog(
            onDismissRequest = { selectedApkResult = null },
            title = {
                Text(
                    text = "🛡 APK Security Scan Result",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "File: ${apkRes.metadata.fileName}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(text = "Package: ${apkRes.metadata.packageName ?: "Unknown"}", fontSize = 13.sp, color = TextSecondary)
                    Text(text = "SHA-256: ${if (apkRes.metadata.sha256Hash.length > 16) apkRes.metadata.sha256Hash.take(16) + "..." else apkRes.metadata.sha256Hash}", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Status: ${apkRes.threatStatusText}", fontWeight = FontWeight.Bold, color = if (apkRes.riskScore > 30) SuspiciousAmber else SafeGreen)
                    if (apkRes.warnings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Heuristic Warnings:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        apkRes.warnings.forEach { warning ->
                            Text(text = "• $warning", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedApkResult = null }) {
                    Text("OK", fontWeight = FontWeight.Bold, color = BrandTeal)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Protection Status Check Summary Dialog
    if (showStatusCheckDialog) {
        val webState = OnlineMonitoringManager.getWebsiteFeatureState(isOnline, settings.websiteMonitoringEnabled)
        val apkState = OnlineMonitoringManager.getApkFeatureState(settings.apkMonitoringEnabled)
        val notifState = OnlineMonitoringManager.getNotificationFeatureState(context, settings.notificationMonitoringEnabled)
        val netState = OnlineMonitoringManager.getNetworkFeatureState(context, settings.networkMonitoringEnabled)
        val threatState = OnlineMonitoringManager.getThreatUpdatesFeatureState(isOnline, settings.automaticThreatUpdatesEnabled)
        val alertsState = OnlineMonitoringManager.getSecurityAlertsFeatureState(context, settings.securityAlertsEnabled)

        AlertDialog(
            onDismissRequest = { showStatusCheckDialog = false },
            title = {
                Text(
                    text = "🔍 Real-Time Protection Audit",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatusSummaryRow("Internet Connection", if (isOnline) "Connected" else "Offline", isOnline)
                    StatusSummaryRow("Website Protection", webState.status.label, webState.status == ProtectionFeatureStatus.ACTIVE)
                    StatusSummaryRow("APK Monitoring", apkState.status.label, apkState.status == ProtectionFeatureStatus.ACTIVE)
                    StatusSummaryRow("Notification Scam Detection", notifState.status.label, notifState.status == ProtectionFeatureStatus.ACTIVE)
                    StatusSummaryRow("Network Protection (VPN)", if (isVpnRunning) "Active" else netState.status.label, isVpnRunning)
                    StatusSummaryRow("Threat Database Updates", threatState.status.label, threatState.status == ProtectionFeatureStatus.ACTIVE)
                    StatusSummaryRow("Security Alerts", alertsState.status.label, alertsState.status == ProtectionFeatureStatus.ACTIVE)
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusCheckDialog = false }) {
                    Text("Close Audit", fontWeight = FontWeight.Bold, color = BrandTeal)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Main Page Content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.privacy_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp
                    )
                )
                Text(
                    text = "Independent Protection Controls",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                )
            }
            OfflineStatusBadge()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Check Protection Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrandTealLight,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = BrandTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Protection Audit",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = "Verify runtime states & permissions",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showStatusCheckDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BrandTeal)
                ) {
                    Text("Check Status", color = BrandTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Language Selector Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLanguageDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrandTealLight,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = BrandTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.setting_language_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = langDisplay,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BrandTeal,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "SECURITY CONTROLS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = BrandTeal,
                letterSpacing = 1.sp,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Control 1: Website & Browser Monitoring
        val hasAccessibility = remember(refreshTrigger) {
            OnlineMonitoringManager.hasAccessibilityPermission(context)
        }
        val webState = remember(settings.websiteMonitoringEnabled, isOnline) {
            OnlineMonitoringManager.getWebsiteFeatureState(isOnline, settings.websiteMonitoringEnabled)
        }
        MonitoringControlCard(
            title = "Website Monitoring",
            description = "Check URLs typed or visited in Chrome & browsers against local rules and online threat feeds.",
            icon = Icons.Default.Web,
            isChecked = settings.websiteMonitoringEnabled,
            featureState = webState,
            onCheckedChange = { SecurityPreferencesRepository.setWebsiteMonitoringEnabled(it) },
            actionButton = if (settings.websiteMonitoringEnabled && !hasAccessibility) {
                {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Throwable) {
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BrandTeal)
                    ) {
                        Text("Enable Real-Time Browser Monitor", color = BrandTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            } else null
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Control 2: APK & App Installation Monitoring
        val apkState = remember(settings.apkMonitoringEnabled) {
            OnlineMonitoringManager.getApkFeatureState(settings.apkMonitoringEnabled)
        }
        MonitoringControlCard(
            title = "APK & App Installation Monitoring",
            description = "Monitor supported APK download and installation events and scan accessible files for security risks.",
            icon = Icons.Default.Android,
            isChecked = settings.apkMonitoringEnabled,
            featureState = apkState,
            onCheckedChange = { SecurityPreferencesRepository.setApkMonitoringEnabled(it) },
            actionButton = {
                OutlinedButton(
                    onClick = { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BrandTeal)
                ) {
                    Text("Select & Scan APK File...", color = BrandTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Control 3: Notification Scam Monitoring
        val notifState = remember(settings.notificationMonitoringEnabled, refreshTrigger) {
            OnlineMonitoringManager.getNotificationFeatureState(context, settings.notificationMonitoringEnabled)
        }
        MonitoringControlCard(
            title = "Notification Scam Detection",
            description = "Analyze notifications you authorize RakshaAI to access for scam and phishing indicators directly on device.",
            icon = Icons.Default.Notifications,
            isChecked = settings.notificationMonitoringEnabled,
            featureState = notifState,
            onCheckedChange = { SecurityPreferencesRepository.setNotificationMonitoringEnabled(it) },
            actionButton = if (settings.notificationMonitoringEnabled && !notifState.hasPermission) {
                {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Throwable) {
                                try {
                                    val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(fallbackIntent)
                                } catch (_: Throwable) {
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BrandTeal)
                    ) {
                        Text("Enable Notification Access", color = BrandTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            } else null
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Control 4: Network Connection Monitoring
        val netState = remember(settings.networkMonitoringEnabled, isVpnRunning, refreshTrigger) {
            OnlineMonitoringManager.getNetworkFeatureState(context, settings.networkMonitoringEnabled)
        }
        MonitoringControlCard(
            title = "Network Connection Monitoring",
            description = "Optionally monitor supported network connection metadata using RakshaAI's local security VPN.",
            icon = Icons.Default.VpnKey,
            isChecked = settings.networkMonitoringEnabled,
            featureState = netState,
            onCheckedChange = { enabled ->
                if (enabled) {
                    val prepareIntent = VpnService.prepare(context)
                    if (prepareIntent != null) {
                        vpnPrepareLauncher.launch(prepareIntent)
                    } else {
                        VpnManager.startVpnService(context)
                    }
                } else {
                    VpnManager.stopVpnService(context)
                }
                SecurityPreferencesRepository.setNetworkMonitoringEnabled(enabled)
            },
            actionButton = if (settings.networkMonitoringEnabled && !netState.hasPermission) {
                {
                    OutlinedButton(
                        onClick = {
                            val prepareIntent = VpnService.prepare(context)
                            if (prepareIntent != null) {
                                vpnPrepareLauncher.launch(prepareIntent)
                            } else {
                                VpnManager.startVpnService(context)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BrandTeal)
                    ) {
                        Text("Authorize Local VPN", color = BrandTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            } else null
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Control 5: Automatic Threat Database Updates
        val threatState = remember(settings.automaticThreatUpdatesEnabled, isOnline) {
            OnlineMonitoringManager.getThreatUpdatesFeatureState(isOnline, settings.automaticThreatUpdatesEnabled)
        }
        MonitoringControlCard(
            title = "Automatic Threat Database Updates",
            description = "Automatically refresh threat intelligence database when connected to the internet.",
            icon = Icons.Default.Refresh,
            isChecked = settings.automaticThreatUpdatesEnabled,
            featureState = threatState,
            onCheckedChange = { SecurityPreferencesRepository.setAutomaticThreatUpdatesEnabled(it) },
            actionButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isManualUpdating = true
                                manualUpdateStatusText = "Refreshing..."
                                ThreatIntelligenceStore.initialize(context)
                                isManualUpdating = false
                                manualUpdateStatusText = "Updated successfully"
                            }
                        },
                        enabled = !isManualUpdating && isOnline,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BrandTeal)
                    ) {
                        if (isManualUpdating) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = BrandTeal)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Refresh Threat Feed Now", color = BrandTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    manualUpdateStatusText?.let { txt ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = txt, fontSize = 11.sp, color = SafeGreen)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Control 6: Security Alerts
        val alertsState = remember(settings.securityAlertsEnabled, refreshTrigger) {
            OnlineMonitoringManager.getSecurityAlertsFeatureState(context, settings.securityAlertsEnabled)
        }
        MonitoringControlCard(
            title = "Security Alerts",
            description = "Display real-time security alerts and notifications when suspicious threats are detected.",
            icon = Icons.Default.Notifications,
            isChecked = settings.securityAlertsEnabled,
            featureState = alertsState,
            onCheckedChange = { SecurityPreferencesRepository.setSecurityAlertsEnabled(it) },
            actionButton = if (settings.securityAlertsEnabled && !alertsState.hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                {
                    OutlinedButton(
                        onClick = {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BrandTeal)
                    ) {
                        Text("Grant Notification Permission", color = BrandTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            } else null
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Privacy Checklist Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrandTealLight,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = BrandTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.offline_mode_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = 0.8.sp,
                            fontSize = 15.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                PrivacyCheckItem(text = stringResource(R.string.offline_check_1))
                Spacer(modifier = Modifier.height(10.dp))
                PrivacyCheckItem(text = stringResource(R.string.offline_check_2))
                Spacer(modifier = Modifier.height(10.dp))
                PrivacyCheckItem(text = stringResource(R.string.offline_check_3))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // About Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = BrandTeal,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "RakshaAI Security v1.1",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp
                        )
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun MonitoringControlCard(
    title: String,
    description: String,
    icon: ImageVector,
    isChecked: Boolean,
    featureState: FeatureState,
    onCheckedChange: (Boolean) -> Unit,
    actionButton: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isChecked) BrandTealLight else MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isChecked) BrandTeal else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp
                        )
                    )
                }

                Switch(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.surface,
                        checkedTrackColor = BrandTeal,
                        uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                        uncheckedTrackColor = TextSecondary.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status indicator badge row
            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusColor = when (featureState.status) {
                    ProtectionFeatureStatus.ACTIVE -> SafeGreen
                    ProtectionFeatureStatus.WAITING_FOR_INTERNET -> SuspiciousAmber
                    ProtectionFeatureStatus.PERMISSION_REQUIRED -> SuspiciousAmber
                    ProtectionFeatureStatus.DISABLED -> TextSecondary
                    ProtectionFeatureStatus.NOT_CONFIGURED -> TextSecondary
                    ProtectionFeatureStatus.FAILED_TO_START -> SuspiciousAmber
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${featureState.status.label}: ${featureState.statusMessage}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        fontSize = 12.sp
                    )
                )
            }

            if (actionButton != null) {
                Spacer(modifier = Modifier.height(12.dp))
                actionButton()
            }
        }
    }
}

@Composable
fun StatusSummaryRow(label: String, value: String, isActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) SafeGreen else SuspiciousAmber
        )
    }
}

@Composable
fun LanguageOptionRow(
    code: String,
    label: String,
    currentSelected: String,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = code == currentSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = BrandTeal)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (code == currentSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}

@Composable
fun PrivacyCheckItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = SafeGreen.copy(alpha = 0.15f),
            modifier = Modifier.size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}
