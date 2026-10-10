package com.rakshaai.security.ui.result

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshaai.security.R
import com.rakshaai.security.data.model.ContentType
import com.rakshaai.security.data.model.RiskLevel
import com.rakshaai.security.data.model.ScanResult
import com.rakshaai.security.ui.components.CategoryChip
import com.rakshaai.security.ui.components.OfflineStatusBadge
import com.rakshaai.security.ui.theme.BorderSubtle
import com.rakshaai.security.ui.theme.BrandTeal
import com.rakshaai.security.ui.theme.BrandTealLight
import com.rakshaai.security.ui.theme.RiskDanger
import com.rakshaai.security.ui.theme.RiskRed
import com.rakshaai.security.ui.theme.SafeGreen
import com.rakshaai.security.ui.theme.SuspiciousAmber
import com.rakshaai.security.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ResultScreen(
    scanResult: ScanResult,
    onNavigateBack: () -> Unit,
    onScanAnother: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showTechnicalDetails by remember { mutableStateOf(false) }
    var showUnsafeWarningDialog by remember(scanResult.id) {
        mutableStateOf(scanResult.riskLevel != RiskLevel.SAFE)
    }

    val riskColor: Color = when (scanResult.riskLevel) {
        RiskLevel.SAFE -> SafeGreen
        RiskLevel.SUSPICIOUS -> SuspiciousAmber
        RiskLevel.HIGH_RISK -> RiskRed
        RiskLevel.DANGEROUS -> RiskDanger
    }

    val riskIcon: ImageVector = when (scanResult.riskLevel) {
        RiskLevel.SAFE -> Icons.Default.CheckCircle
        RiskLevel.SUSPICIOUS -> Icons.Default.Warning
        RiskLevel.HIGH_RISK, RiskLevel.DANGEROUS -> Icons.Default.Security
    }

    val riskLevelText = when (scanResult.riskLevel) {
        RiskLevel.SAFE -> stringResource(R.string.risk_safe)
        RiskLevel.SUSPICIOUS -> stringResource(R.string.risk_suspicious)
        RiskLevel.HIGH_RISK -> stringResource(R.string.risk_high_risk)
        RiskLevel.DANGEROUS -> stringResource(R.string.risk_dangerous)
    }

    if (showUnsafeWarningDialog) {
        val dialogTitle = when (scanResult.riskLevel) {
            RiskLevel.DANGEROUS -> "🔴 DANGEROUS WEBSITE DETECTED"
            RiskLevel.HIGH_RISK -> "⚠ HIGH-RISK WEBSITE WARNING"
            RiskLevel.SUSPICIOUS -> "⚡ SUSPICIOUS WEBSITE WARNING"
            RiskLevel.SAFE -> ""
        }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUnsafeWarningDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (scanResult.riskLevel == RiskLevel.DANGEROUS) RiskDanger else RiskRed,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = dialogTitle,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (scanResult.riskLevel == RiskLevel.DANGEROUS) RiskDanger else RiskRed
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = scanResult.rawContent,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "Risk Assessment: ${scanResult.riskScore} / 100 (${scanResult.riskLevel.label})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (scanResult.riskLevel == RiskLevel.DANGEROUS) RiskDanger else RiskRed
                    )
                    if (scanResult.signals.isNotEmpty()) {
                        Text(text = "Detected Risk Signals:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        scanResult.signals.take(3).forEach { signal ->
                            Text(text = "• ${signal.title}: ${signal.description}", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⛔ SAFETY ADVISORY: Do NOT visit this link or enter passwords, credit cards, or banking details.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showUnsafeWarningDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = if (scanResult.riskLevel == RiskLevel.DANGEROUS) RiskDanger else RiskRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("I Understand — Review Analysis", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = stringResource(R.string.result_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp
                    )
                )
            }
            OfflineStatusBadge()
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (scanResult.isNotification) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandTealLight),
                border = BorderStroke(1.dp, BrandTeal.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = BrandTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Source: Notification Protection",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp
                            )
                        )
                        Text(
                            text = "Analyzed locally by RakshaAI",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // 1. RISK STATUS SECTION
        Text(
            text = stringResource(R.string.section_risk_status),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = BrandTeal,
                letterSpacing = 1.sp,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, riskColor.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(riskColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = riskIcon,
                        contentDescription = null,
                        tint = riskColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = riskLevelText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = riskColor,
                        fontSize = 26.sp,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.risk_score_format, scanResult.riskScore),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 17.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { scanResult.riskScore / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = riskColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryChip(category = scanResult.category)

                    val mlBadgeColor = if (scanResult.mlClassificationLabel == "SCAM") RiskRed else SafeGreen
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = mlBadgeColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, mlBadgeColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = mlBadgeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ML Detection: ${scanResult.mlClassificationLabel}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = mlBadgeColor,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 2. WHAT WE FOUND SECTION
        Text(
            text = stringResource(R.string.section_what_we_found),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = BrandTeal,
                letterSpacing = 1.sp,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                val highlightedText = buildHighlightedText(
                    rawContent = scanResult.rawContent,
                    highlightedPhrases = scanResult.highlightedPhrases,
                    highlightColor = riskColor
                )

                Text(
                    text = highlightedText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 3. WHY IS THIS RISKY?
        Text(
            text = stringResource(R.string.section_why_is_this_risky),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                scanResult.signals.forEach { signal ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "•",
                            fontWeight = FontWeight.Bold,
                            color = riskColor,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = signal.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 15.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = signal.description,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. WARNING SIGNS CHIPS
        if (scanResult.warningSigns.isNotEmpty()) {
            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = stringResource(R.string.section_warning_signs),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                scanResult.warningSigns.forEach { warningTag ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RiskRed.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, RiskRed.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = warningTag,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = RiskRed,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 5. WHAT SHOULD YOU DO?
        Text(
            text = stringResource(R.string.section_what_should_you_do),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (scanResult.riskLevel == RiskLevel.HIGH_RISK || scanResult.riskLevel == RiskLevel.DANGEROUS)
                    RiskRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                scanResult.recommendedActions.forEach { action ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "✓",
                            fontWeight = FontWeight.Bold,
                            color = BrandTeal,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = action,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 15.sp,
                                lineHeight = 21.sp
                            )
                        )
                    }
                }
            }
        }

        // 6. HOW TO AVOID THIS
        if (scanResult.preventionGuidance.isNotBlank()) {
            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = stringResource(R.string.section_how_to_avoid_this),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandTealLight.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, BrandTeal.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = BrandTeal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = scanResult.preventionGuidance,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Collapsible Technical Details Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTechnicalDetails = !showTechnicalDetails },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "View technical details",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandTeal,
                            fontSize = 14.sp
                        )
                    )
                    Icon(
                        imageVector = if (showTechnicalDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = BrandTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = showTechnicalDetails) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = "Analyzed Content (${if (scanResult.isNotification) "Notification" else scanResult.contentType.displayName}):",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (scanResult.isNotification) "Notification content analyzed privately." else scanResult.rawContent,
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 13.sp),
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        if (scanResult.isNotification) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your notification was analyzed on this device. RakshaAI does not upload your notification content.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            )
                        }

                        if (scanResult.mlScore != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "SVM Decision Margin Score: ${String.format(Locale.ROOT, "%.5f", scanResult.mlScore)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onScanAnother,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BrandTeal)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = BrandTeal,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_scan_again),
                    style = MaterialTheme.typography.labelLarge.copy(color = BrandTeal, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                )
            }

            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
            ) {
                Text(
                    text = stringResource(R.string.btn_done),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun buildHighlightedText(
    rawContent: String,
    highlightedPhrases: List<String>,
    highlightColor: Color
): AnnotatedString {
    if (highlightedPhrases.isEmpty()) {
        return AnnotatedString(rawContent)
    }

    return buildAnnotatedString {
        var currentIndex = 0
        val lowerContent = rawContent.lowercase(Locale.ROOT)

        // Find all phrase occurrences
        val matches = mutableListOf<Pair<Int, Int>>() // start to end
        for (phrase in highlightedPhrases) {
            if (phrase.isBlank()) continue
            val lowerPhrase = phrase.lowercase(Locale.ROOT)
            var start = lowerContent.indexOf(lowerPhrase)
            while (start != -1) {
                matches.add(start to (start + phrase.length))
                start = lowerContent.indexOf(lowerPhrase, start + 1)
            }
        }

        // Sort matches by start index
        val sortedMatches = matches.sortedBy { it.first }

        for ((start, end) in sortedMatches) {
            if (start >= currentIndex) {
                append(rawContent.substring(currentIndex, start))
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = highlightColor,
                        background = highlightColor.copy(alpha = 0.15f)
                    )
                ) {
                    append(rawContent.substring(start, end))
                }
                currentIndex = end
            }
        }

        if (currentIndex < rawContent.length) {
            append(rawContent.substring(currentIndex))
        }
    }
}
