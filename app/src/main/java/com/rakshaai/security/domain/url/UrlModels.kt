package com.rakshaai.security.domain.url

import com.rakshaai.security.data.model.RiskLevel

data class ParsedUrl(
    val originalUrl: String,
    val normalizedUrl: String,
    val scheme: String,
    val host: String,
    val port: Int,
    val path: String,
    val query: String?,
    val fragment: String?,
    val userInfo: String?,
    val authority: String?
)

data class UrlFeatures(
    val isHttps: Boolean,
    val isHttp: Boolean,
    val isIpAddress: Boolean,
    val hasUserInfo: Boolean,
    val isPunycode: Boolean,
    val isUnicodeHost: Boolean,
    val isShortener: Boolean,
    val isSuspiciousTld: Boolean,
    val tldName: String?,
    val subdomainDepth: Int,
    val isDeepSubdomain: Boolean,
    val hasUnusualPort: Boolean,
    val isVeryLongUrl: Boolean,
    val brandImpersonated: String?,
    val hasSuspiciousStructure: Boolean
)

enum class ThreatMatchLevel {
    EXACT_URL_MATCH,
    HOSTNAME_MATCH,
    NO_MATCH
}

data class ThreatIntelResult(
    val matchLevel: ThreatMatchLevel,
    val isMatched: Boolean,
    val matchedItem: String?,
    val detailMessage: String
)

data class UrlAnalysisResult(
    val originalUrl: String,
    val normalizedUrl: String,
    val host: String,
    val score: Int, // 0 to 100
    val riskLevel: RiskLevel,
    val threatIntelResult: ThreatIntelResult,
    val features: UrlFeatures,
    val signals: List<String>,
    val warningTags: List<String>,
    val explanation: String
)
