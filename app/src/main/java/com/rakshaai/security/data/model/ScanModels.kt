package com.rakshaai.security.data.model

import java.util.UUID

enum class ContentType(val displayName: String) {
    MESSAGE("Message"),
    URL("Web URL"),
    SCREENSHOT("Screenshot"),
    PHOTO("Photo")
}

enum class RiskLevel(val label: String) {
    SAFE("SAFE"),
    SUSPICIOUS("SUSPICIOUS"),
    HIGH_RISK("HIGH RISK"),
    DANGEROUS("DANGEROUS")
}

enum class RiskCategory(val title: String, val description: String) {
    BANKING_KYC("Banking / KYC Scam", "Impersonates official bank or urgent KYC update request"),
    UPI_PAYMENT("UPI / Payment Fraud", "Tricks user into sending money or authorizing fraudulent UPI request"),
    COURIER_PARCEL("Courier / Parcel Scam", "Fake delivery notification demanding payment or personal info"),
    JOB_INVESTMENT("Job / Investment Scam", "Unrealistic daily returns or part-time job offer lure"),
    LOTTERY_PRIZE("Lottery / Prize Scam", "Claims reward or prize requiring processing fee or details"),
    PHISHING("Phishing / Malicious URL", "Deceptive link disguised to steal banking or account credentials"),
    SAFE_MSG("Normal Communication", "No significant threat indicators detected")
}

data class RiskSignal(
    val title: String,
    val description: String,
    val isPresent: Boolean = true
)

data class ScanResult(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val contentType: ContentType,
    val rawContent: String,
    val imageUri: String? = null,
    val language: String = "English",
    val riskScore: Int, // 0 to 100
    val riskLevel: RiskLevel,
    val category: RiskCategory,
    val mlClassificationLabel: String = "BENIGN",
    val mlScore: Double? = null,
    val signals: List<RiskSignal>,
    val highlightedPhrases: List<String> = emptyList(),
    val warningSigns: List<String> = emptyList(),
    val recommendedActions: List<String>,
    val preventionGuidance: String = "",
    val isOfflineProcessed: Boolean = true,
    val isNotification: Boolean = false
)
