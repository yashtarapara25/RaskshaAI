package com.rakshaai.security.domain.url

import com.rakshaai.security.data.model.RiskLevel

object UrlRuleEngine {

    fun evaluate(
        parsedUrl: ParsedUrl,
        features: UrlFeatures,
        threatIntel: ThreatIntelResult
    ): UrlAnalysisResult {
        val signals = mutableListOf<String>()
        val warningTags = mutableListOf<String>()
        var score = 0

        // 1. Threat Intelligence Matching
        if (threatIntel.isMatched) {
            when (threatIntel.matchLevel) {
                ThreatMatchLevel.EXACT_URL_MATCH -> {
                    score += 60
                    signals.add("Matched known phishing URL in local PhishTank database.")
                    warningTags.add("PhishTank Match")
                }
                ThreatMatchLevel.HOSTNAME_MATCH -> {
                    score += 50
                    signals.add("Host domain '${threatIntel.matchedItem}' matched known phishing domain in local PhishTank database.")
                    warningTags.add("PhishTank Domain Match")
                }
                ThreatMatchLevel.NO_MATCH -> {}
            }
        }

        // 2. IP Address Hostname (+25)
        if (features.isIpAddress) {
            score += 25
            signals.add("URL host is a raw IP address instead of a domain name.")
            warningTags.add("Raw IP Host")
        }

        // 3. User-Info / @ Symbol (+25)
        if (features.hasUserInfo) {
            score += 25
            signals.add("URL contains '@' symbol or user-info credential redirect.")
            warningTags.add("User-Info Redirect")
        }

        // 4. Punycode Host (+20)
        if (features.isPunycode) {
            score += 20
            signals.add("Host uses Punycode ('xn--') encoding, often used in IDN homograph attacks.")
            warningTags.add("Punycode Homograph")
        }

        // 5. Brand Impersonation in Host (+20)
        features.brandImpersonated?.let { brand ->
            score += 20
            signals.add("Host domain attempts to impersonate brand '$brand'.")
            warningTags.add("Brand Impersonation")
        }

        // 6. Suspicious TLD (+10)
        if (features.isSuspiciousTld) {
            score += 10
            signals.add("Uses an untrusted high-risk top-level domain extension (.${features.tldName}).")
            warningTags.add("Suspicious TLD")
        }

        // 7. URL Shortener (+10)
        if (features.isShortener) {
            score += 10
            signals.add("URL shortener used to disguise the real destination domain.")
            warningTags.add("URL Shortener")
        }

        // 8. Deep Subdomain (+10)
        if (features.isDeepSubdomain) {
            score += 10
            signals.add("Contains an unusually deep subdomain structure (${features.subdomainDepth} levels).")
            warningTags.add("Deep Subdomain")
        }

        // 9. Unusual Port (+10)
        if (features.hasUnusualPort) {
            score += 10
            signals.add("Uses non-standard network port (${parsedUrl.port}).")
            warningTags.add("Unusual Port")
        }

        // 10. Non-HTTPS / HTTP (+5)
        if (features.isHttp) {
            score += 5
            signals.add("URL uses insecure HTTP protocol instead of HTTPS.")
            warningTags.add("Insecure HTTP")
        }

        // 11. Very Long URL (+5)
        if (features.isVeryLongUrl) {
            score += 5
            signals.add("Exceeds standard URL length (over 100 characters).")
            warningTags.add("Long URL")
        }

        // 12. Suspicious Structure (+5)
        if (features.hasSuspiciousStructure && !features.isIpAddress && !features.isPunycode) {
            score += 5
            signals.add("URL displays an irregular technical domain structure.")
        }

        // Default explanation when clean
        if (signals.isEmpty()) {
            signals.add("No structural threat or phishing signals detected on domain '${parsedUrl.host}'.")
        }

        val finalScore = score.coerceIn(0, 100)

        val riskLevel = when {
            finalScore >= 80 -> RiskLevel.DANGEROUS
            finalScore >= 60 -> RiskLevel.HIGH_RISK
            finalScore >= 30 -> RiskLevel.SUSPICIOUS
            else -> RiskLevel.SAFE
        }

        val explanation = when {
            threatIntel.isMatched -> "Flagged by PhishTank offline threat-intelligence database."
            features.brandImpersonated != null -> "Domain structure indicates potential brand impersonation."
            features.isIpAddress || features.hasUserInfo -> "URL contains high-risk technical features (IP/credentials)."
            finalScore >= 30 -> "URL exhibits multiple warning indicators (shortener, TLD, deep subdomains)."
            else -> "URL host and structure appear safe. ${threatIntel.detailMessage}"
        }

        return UrlAnalysisResult(
            originalUrl = parsedUrl.originalUrl,
            normalizedUrl = parsedUrl.normalizedUrl,
            host = parsedUrl.host,
            score = finalScore,
            riskLevel = riskLevel,
            threatIntelResult = threatIntel,
            features = features,
            signals = signals,
            warningTags = warningTags,
            explanation = explanation
        )
    }
}
