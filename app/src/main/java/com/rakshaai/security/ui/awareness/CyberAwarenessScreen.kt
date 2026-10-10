package com.rakshaai.security.ui.awareness

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.rakshaai.security.data.preference.LanguageManager
import com.rakshaai.security.ui.components.OfflineStatusBadge
import com.rakshaai.security.ui.theme.*
import java.io.File
import java.io.FileOutputStream

@Composable
fun CyberAwarenessScreen(
    onNavigateToScan: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // Rule Book Language State (defaults to app language or device locale)
    val appLang = LanguageManager.getLanguage(context)
    var selectedRuleBookLang by remember {
        mutableStateOf(
            when (appLang) {
                "hi" -> "hi"
                "gu" -> "gu"
                else -> "en"
            }
        )
    }

    // Checklist state stored in SharedPreferences
    val prefs = remember { context.getSharedPreferences("rakshaai_awareness_prefs", Context.MODE_PRIVATE) }
    var checklistState by remember {
        mutableStateOf(
            mapOf(
                1 to prefs.getBoolean("check_1", false),
                2 to prefs.getBoolean("check_2", false),
                3 to prefs.getBoolean("check_3", false),
                4 to prefs.getBoolean("check_4", false),
                5 to prefs.getBoolean("check_5", false),
                6 to prefs.getBoolean("check_6", false)
            )
        )
    }

    fun updateCheck(id: Int, value: Boolean) {
        checklistState = checklistState.toMutableMap().apply { this[id] = value }
        prefs.edit().putBoolean("check_$id", value).apply()
    }

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
                    text = "Cyber Awareness",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp
                    )
                )
                Text(
                    text = "Your Digital Safety Guide",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                )
            }
            OfflineStatusBadge()
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Hero Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = BrandTeal),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Raksha AI",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Your Digital Safety Partner",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Think Before You Click. Stay Safe with Raksha AI.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Detect suspicious messages, check risky links, and stay safe online.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION: RECOGNIZE CYBER THREATS
        SectionHeader(title = "Recognize Cyber Threats", subtitle = "Know what scams look like")
        Spacer(modifier = Modifier.height(12.dp))

        val threats = listOf(
            Triple("Unexpected Urgency", "Scammers create fear by saying your account will be blocked or you must act immediately.", Icons.Default.Schedule),
            Triple("Fake Authority", "Someone pretends to represent your bank, police, courier company, KYC service, or government.", Icons.Default.VerifiedUser),
            Triple("Suspicious Links", "Unexpected, shortened, misspelled, or unfamiliar web addresses leading to phishing pages.", Icons.Default.Link),
            Triple("OTP, PIN, or Password Requests", "Never share your OTP, PIN, CVV, or password with another person under any circumstance.", Icons.Default.VpnKey),
            Triple("Too-Good-to-Be-True Offers", "Unexpected lottery wins, guaranteed returns, easy jobs, and requests for advance fees.", Icons.Default.CardGiftcard),
            Triple("Unknown Apps & Screen Sharing", "Do not install unknown APKs or screen-sharing apps requested by unsolicited callers.", Icons.Default.PhoneAndroid)
        )

        threats.forEachIndexed { index, (title, desc, icon) ->
            ThreatCard(number = index + 1, title = title, description = desc, icon = icon)
            if (index < threats.size - 1) Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: PREVENT CYBER ATTACKS (8 Golden Rules + Stop Think Verify)
        SectionHeader(title = "Prevent Cyber Attacks", subtitle = "The 8 Golden Rules of Cyber Safety")
        Spacer(modifier = Modifier.height(12.dp))

        // Stop, Think, Verify Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = BrandTealLight),
            border = BorderStroke(1.dp, BrandTeal.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🛑 ➔ 🧠 ➔ ✅  STOP • THINK • VERIFY",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandTeal,
                        fontSize = 14.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• STOP: Do not react out of fear or excitement.\n• THINK: Check who sent the message and whether it requests money, OTPs, or sensitive info.\n• VERIFY: Use the official app, website, or verified phone number.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val goldenRules = listOf(
            "Never share OTP, PIN, CVV, or passwords with anyone.",
            "Pause for 10 seconds before acting on an urgent message.",
            "Do not open unexpected or unfamiliar links.",
            "Never enter your UPI PIN just to receive money.",
            "Do not install apps at the request of an unknown caller.",
            "Verify messages using official apps, websites, or phone numbers.",
            "Be suspicious of lottery prizes and jobs requiring upfront fees.",
            "Use strong unique passwords, enable 2FA, and keep your phone updated."
        )

        goldenRules.forEachIndexed { index, rule ->
            GoldenRuleCard(number = index + 1, rule = rule)
            if (index < goldenRules.size - 1) Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: FRAUD HAPPENED? ACT QUICKLY
        SectionHeader(title = "Fraud Happened? Act Quickly.", subtitle = "Emergency response steps")
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5f.dp, RiskDanger.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                val steps = listOf(
                    "1. STOP — Stop responding and do not send more money. Preserve messages, phone numbers, links, and receipts.",
                    "2. CONTACT YOUR BANK — Contact your bank or payment provider immediately via official channels to freeze transactions.",
                    "3. CALL 1930 — Report financial cyber fraud promptly through the National Cyber Crime Helpline.",
                    "4. REPORT ONLINE — Visit cybercrime.gov.in and keep your complaint acknowledgement number safe.",
                    "5. SECURE ACCOUNTS — Change compromised passwords on a trusted device and remove unfamiliar apps.",
                    "6. MONITOR — Regularly check account statements for unauthorized activity."
                )

                steps.forEachIndexed { idx, step ->
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    )
                    if (idx < steps.size - 1) Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RiskDanger.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "⚠ Never pay anyone who promises guaranteed recovery of your stolen money.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = RiskDanger,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // External Resource Links
                Text(
                    text = "Official Portals & Helplines:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandTeal,
                        fontSize = 13.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                ResourceLinkRow(label = "National Cyber Crime Reporting Portal", url = "https://cybercrime.gov.in/") {
                    openUrl(context, "https://cybercrime.gov.in/")
                }
                Spacer(modifier = Modifier.height(6.dp))
                ResourceLinkRow(label = "CERT-In (Indian Computer Emergency Response Team)", url = "https://www.cert-in.org.in/") {
                    openUrl(context, "https://www.cert-in.org.in/")
                }
                Spacer(modifier = Modifier.height(6.dp))
                ResourceLinkRow(label = "Sanchar Saathi (Telecom Fraud Reporting)", url = "https://sancharsaathi.gov.in/") {
                    openUrl(context, "https://sancharsaathi.gov.in/")
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: CHOOSE YOUR CYBER SAFETY RULE BOOK (PDF Reader)
        SectionHeader(title = "Cyber Safety Rule Book", subtitle = "Official offline guide in 3 languages")
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BrandTealLight,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = BrandTeal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Cyber Safety Rule Book",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Read offline in English, Hindi, or Gujarati",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Language Selection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RuleBookLangButton(
                        label = "English",
                        isSelected = selectedRuleBookLang == "en",
                        modifier = Modifier.weight(1f)
                    ) { selectedRuleBookLang = "en" }

                    RuleBookLangButton(
                        label = "हिंदी",
                        isSelected = selectedRuleBookLang == "hi",
                        modifier = Modifier.weight(1f)
                    ) { selectedRuleBookLang = "hi" }

                    RuleBookLangButton(
                        label = "ગુજરાતી",
                        isSelected = selectedRuleBookLang == "gu",
                        modifier = Modifier.weight(1f)
                    ) { selectedRuleBookLang = "gu" }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val ruleBookTitle = when (selectedRuleBookLang) {
                    "hi" -> "पूरी हिंदी साइबर सुरक्षा नियम पुस्तिका पढ़ें"
                    "gu" -> "સંપૂર્ણ ગુજરાતી સાયબર સુરક્ષા નિયમ પુસ્તિકા વાંચો"
                    else -> "Read the Complete English Cyber Safety Rule Book"
                }

                val btnText = when (selectedRuleBookLang) {
                    "hi" -> "हिंदी PDF खोलें"
                    "gu" -> "ગુજરાતી PDF ખોલો"
                    else -> "Open English PDF"
                }

                val fileName = when (selectedRuleBookLang) {
                    "hi" -> "CyberSafety_RuleBook_Hindi.pdf"
                    "gu" -> "CyberSafety_RuleBook_Gujarati.pdf"
                    else -> "CyberSafety_RuleBook_English.pdf"
                }

                Text(
                    text = ruleBookTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        openPdfFromAssets(context, fileName)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = btnText,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: INTERACTIVE CYBER SAFETY CHECKLIST
        SectionHeader(title = "Your Cyber Safety Checklist", subtitle = "Tap to track your safety habits")
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                val checklistItems = listOf(
                    1 to "I never share my OTP, PIN, or CVV.",
                    2 to "I check unknown links before opening them.",
                    3 to "I verify messages using official channels.",
                    4 to "I use strong passwords and two-step verification.",
                    5 to "I know the Indian cyber fraud helpline number 1930.",
                    6 to "I preserve evidence if fraud occurs."
                )

                checklistItems.forEach { (id, text) ->
                    val checked = checklistState[id] == true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { updateCheck(id, !checked) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { updateCheck(id, it) },
                            colors = CheckboxDefaults.colors(checkedColor = BrandTeal)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION: HOW RAKSHAAI HELPS
        SectionHeader(title = "How Raksha AI Helps", subtitle = "On-device detection capabilities")
        Spacer(modifier = Modifier.height(12.dp))

        val features = listOf(
            Triple("Message Scanner", "Analyze SMS & WhatsApp messages for potential scam indicators.", Icons.Default.Message),
            Triple("URL Scanner", "Check links for suspicious structures and local PhishTank threat matches.", Icons.Default.Link),
            Triple("Screenshot Scanner", "Extract text from screenshots using on-device OCR and analyze content.", Icons.Default.Image),
            Triple("Risk Analysis", "Provide risk level, warning signs, and recommended next steps.", Icons.Default.Security),
            Triple("Notification Protection", "Analyze incoming notifications in real-time when enabled.", Icons.Default.Notifications),
            Triple("Privacy-First Design", "Designed for 100% on-device analysis and offline use.", Icons.Default.Lock)
        )

        features.forEachIndexed { index, (title, desc, icon) ->
            FeatureGridCard(title = title, description = desc, icon = icon)
            if (index < features.size - 1) Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Scan results are estimates, not guarantees. Always verify suspicious messages through official sources.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        )

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp
            )
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                fontSize = 13.sp
            )
        )
    }
}

@Composable
fun ThreatCard(number: Int, title: String, description: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BrandTealLight,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$number. $title",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                )
            }
        }
    }
}

@Composable
fun GoldenRuleCard(number: Int, rule: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = SafeGreen.copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$number",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SafeGreen,
                            fontSize = 12.sp
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = rule,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ResourceLinkRow(label: String, url: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = BrandTeal,
                fontSize = 13.sp
            ),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = null,
            tint = BrandTeal,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun RuleBookLangButton(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) BrandTeal else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
        ),
        border = BorderStroke(1.dp, if (isSelected) BrandTeal else BorderSubtle)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        )
    }
}

@Composable
fun FeatureGridCard(title: String, description: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BrandTealLight,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Throwable) {
        Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
    }
}

fun openPdfFromAssets(context: Context, fileName: String) {
    try {
        val file = File(context.cacheDir, fileName)
        context.assets.open("awareness/$fileName").use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No PDF reader app found on device.", Toast.LENGTH_LONG).show()
    } catch (e: Throwable) {
        Toast.makeText(context, "Error opening rule book PDF.", Toast.LENGTH_SHORT).show()
    }
}
