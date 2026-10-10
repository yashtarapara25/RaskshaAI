package com.rakshaai.security.ui.dashboard

import android.net.Uri
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshaai.security.R
import com.rakshaai.security.data.model.ContentType
import com.rakshaai.security.data.model.ScanResult
import com.rakshaai.security.data.network.OnlineMonitoringManager
import com.rakshaai.security.data.preference.SecurityPreferencesRepository
import com.rakshaai.security.data.repository.ScanRepository
import com.rakshaai.security.domain.apk.ApkMonitoringManager
import com.rakshaai.security.domain.apk.ApkScanResult
import com.rakshaai.security.ui.components.CategoryChip
import com.rakshaai.security.ui.components.OfflineStatusBadge
import com.rakshaai.security.ui.components.RiskScoreBadge
import com.rakshaai.security.ui.components.ScanOptionItem
import com.rakshaai.security.ui.theme.BorderSubtle
import com.rakshaai.security.ui.theme.BrandTeal
import com.rakshaai.security.ui.theme.BrandTealLight
import com.rakshaai.security.ui.theme.SafeGreen
import com.rakshaai.security.ui.theme.SuspiciousAmber
import com.rakshaai.security.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    onNavigateToScan: (ContentType) -> Unit,
    onNavigateToResult: (ScanResult) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val history by ScanRepository.history.collectAsState()
    val protectionStatus by OnlineMonitoringManager.protectionStatus.collectAsState()
    val settings by SecurityPreferencesRepository.settingsState.collectAsState()
    val scrollState = rememberScrollState()

    var selectedApkResult by remember { mutableStateOf<ApkScanResult?>(null) }

    // SAF Launcher for direct APK Scanning from Dashboard
    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            coroutineScope.launch {
                val fileName = "scanned_download.apk"
                val result = ApkMonitoringManager.scanApkUri(context, fileUri, fileName)
                selectedApkResult = result
            }
        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // App Top Header (Shield Logo + Title + Offline Badge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandTealLight,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "RakshaAI Logo",
                            tint = BrandTeal,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 22.sp
                        )
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            OfflineStatusBadge()
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Dynamic Real-time Online Protection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (protectionStatus.isOnlineProtected) BrandTealLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, if (protectionStatus.isOnlineProtected) BrandTeal else BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (protectionStatus.isOnlineProtected) SafeGreen else SuspiciousAmber, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = protectionStatus.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = protectionStatus.subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProtectionFeatureIndicator("Web", settings.websiteMonitoringEnabled)
                    ProtectionFeatureIndicator("APK", settings.apkMonitoringEnabled)
                    ProtectionFeatureIndicator("Notif", settings.notificationMonitoringEnabled)
                    ProtectionFeatureIndicator("VPN", settings.networkMonitoringEnabled)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Section
        Text(
            text = stringResource(R.string.dashboard_hero_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 19.sp
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.dashboard_hero_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Action Section Header
        Text(
            text = stringResource(R.string.dashboard_action_header),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = BrandTeal,
                letterSpacing = 1.sp,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Reusable ScanOptionItem Actions
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ScanOptionItem(
                title = stringResource(R.string.option_message_title),
                description = stringResource(R.string.option_message_desc),
                icon = Icons.Default.Message,
                onClick = { onNavigateToScan(ContentType.MESSAGE) }
            )

            ScanOptionItem(
                title = stringResource(R.string.option_link_title),
                description = stringResource(R.string.option_link_desc),
                icon = Icons.Default.Link,
                onClick = { onNavigateToScan(ContentType.URL) }
            )

            ScanOptionItem(
                title = "APK File Security Scan",
                description = "Select and scan an Android APK file for malware and security risk signals.",
                icon = Icons.Default.Android,
                onClick = { apkPickerLauncher.launch("application/vnd.android.package-archive") }
            )

            ScanOptionItem(
                title = stringResource(R.string.option_screenshot_title),
                description = stringResource(R.string.option_screenshot_desc),
                icon = Icons.Default.Image,
                onClick = { onNavigateToScan(ContentType.SCREENSHOT) }
            )

            ScanOptionItem(
                title = stringResource(R.string.option_voice_title),
                description = stringResource(R.string.option_voice_desc),
                icon = Icons.Default.CameraAlt,
                onClick = { onNavigateToScan(ContentType.PHOTO) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Offline Status Component Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(R.string.offline_mode_title),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandTeal,
                        letterSpacing = 1.sp,
                        fontSize = 12.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OfflineCheckRow(text = stringResource(R.string.offline_check_1))
                Spacer(modifier = Modifier.height(8.dp))
                OfflineCheckRow(text = stringResource(R.string.offline_check_2))
                Spacer(modifier = Modifier.height(8.dp))
                OfflineCheckRow(text = stringResource(R.string.offline_check_3))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Security Activity Preview
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Recent scan",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp
                    )
                )
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandTeal,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.clickable { onNavigateToHistory() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            RecentScanRow(
                scanResult = history.first(),
                onClick = { onNavigateToResult(history.first()) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ProtectionFeatureIndicator(label: String, isActive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(if (isActive) SafeGreen else TextSecondary.copy(alpha = 0.5f), CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.onBackground else TextSecondary
        )
    }
}

@Composable
fun OfflineCheckRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = SafeGreen.copy(alpha = 0.15f),
            modifier = Modifier.size(18.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}

@Composable
fun RecentScanRow(
    scanResult: ScanResult,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryChip(category = scanResult.category)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${scanResult.contentType.displayName}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = scanResult.rawContent,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            RiskScoreBadge(
                riskLevel = scanResult.riskLevel,
                riskScore = scanResult.riskScore
            )
        }
    }
}
