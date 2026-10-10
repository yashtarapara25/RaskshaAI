package com.rakshaai.security.ui.dashboard

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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshaai.security.R
import com.rakshaai.security.data.model.ContentType
import com.rakshaai.security.data.model.ScanResult
import com.rakshaai.security.data.repository.ScanRepository
import com.rakshaai.security.ui.components.CategoryChip
import com.rakshaai.security.ui.components.OfflineStatusBadge
import com.rakshaai.security.ui.components.RiskScoreBadge
import com.rakshaai.security.ui.components.ScanOptionItem
import com.rakshaai.security.ui.theme.BorderSubtle
import com.rakshaai.security.ui.theme.BrandTeal
import com.rakshaai.security.ui.theme.BrandTealLight
import com.rakshaai.security.ui.theme.SafeGreen
import com.rakshaai.security.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    onNavigateToScan: (ContentType) -> Unit,
    onNavigateToResult: (ScanResult) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val history by ScanRepository.history.collectAsState()
    val scrollState = rememberScrollState()

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

        Spacer(modifier = Modifier.height(24.dp))

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

        // 4 Clean Reusable ScanOptionItem Actions
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
