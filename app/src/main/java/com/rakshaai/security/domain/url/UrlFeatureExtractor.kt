package com.rakshaai.security.domain.url

import java.util.Locale

object UrlFeatureExtractor {

    private val SHORTENER_DOMAINS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "buff.ly", "cutt.ly"
    )

    private val SUSPICIOUS_TLDS = setOf(
        "xyz", "top", "cfd", "skin", "sbs", "ru", "tk", "ml", "ga", "gq", "online", "icu"
    )

    private val LEGITIMATE_BRAND_DOMAINS = mapOf(
        "sbi" to setOf("sbi.co.in", "onlinesbi.com", "sbi.com", "sbi.co"),
        "hdfc" to setOf("hdfcbank.com", "hdfc.com"),
        "icici" to setOf("icicibank.com", "icicibank.co.in"),
        "axis" to setOf("axisbank.com"),
        "kotak" to setOf("kotak.com"),
        "paytm" to setOf("paytm.com"),
        "phonepe" to setOf("phonepe.com"),
        "gpay" to setOf("google.com", "pay.google.com"),
        "aadhaar" to setOf("uidai.gov.in"),
        "incometax" to setOf("incometax.gov.in", "incometaxindia.gov.in")
    )

    private val KNOWN_BRANDS = listOf(
        "sbi", "hdfc", "icici", "axis", "kotak", "paytm", "phonepe", "gpay", "aadhaar", "incometax"
    )

    fun extract(parsedUrl: ParsedUrl): UrlFeatures {
        val host = parsedUrl.host.lowercase(Locale.ROOT)
        val rawUrl = parsedUrl.normalizedUrl.lowercase(Locale.ROOT)

        val isHttps = parsedUrl.scheme == "https"
        val isHttp = parsedUrl.scheme == "http"

        val isIpAddress = host.matches(Regex("""^(\d{1,3}\.){3}\d{1,3}$""")) ||
                host.startsWith("[") || host.contains(":")

        val hasUserInfo = parsedUrl.userInfo != null || rawUrl.contains("@")

        val isPunycode = host.contains("xn--")
        val isUnicodeHost = host.any { it.code > 127 }

        var rootHost = host
        if (rootHost.startsWith("www.")) {
            rootHost = rootHost.substring(4)
        }

        val isShortener = SHORTENER_DOMAINS.contains(rootHost)

        val tld = rootHost.substringAfterLast('.', "")
        val isSuspiciousTld = SUSPICIOUS_TLDS.contains(tld)

        val parts = rootHost.split('.')
        val subdomainDepth = if (parts.size > 2) parts.size - 2 else 0
        val isDeepSubdomain = subdomainDepth > 2

        val hasUnusualPort = parsedUrl.port != 80 && parsedUrl.port != 443

        val isVeryLongUrl = rawUrl.length > 100

        // Brand impersonation in HOSTNAME (not path!)
        var brandImpersonated: String? = null
        for (brand in KNOWN_BRANDS) {
            if (host.contains(brand)) {
                val allowedDomains = LEGITIMATE_BRAND_DOMAINS[brand] ?: emptySet()
                val isLegitimate = allowedDomains.any { host == it || host.endsWith(".$it") }
                if (!isLegitimate) {
                    brandImpersonated = brand.uppercase(Locale.ROOT)
                    break
                }
            }
        }

        val hasSuspiciousStructure = isIpAddress || hasUserInfo || isPunycode || (isDeepSubdomain && isSuspiciousTld)

        return UrlFeatures(
            isHttps = isHttps,
            isHttp = isHttp,
            isIpAddress = isIpAddress,
            hasUserInfo = hasUserInfo,
            isPunycode = isPunycode,
            isUnicodeHost = isUnicodeHost,
            isShortener = isShortener,
            isSuspiciousTld = isSuspiciousTld,
            tldName = if (tld.isNotEmpty()) tld else null,
            subdomainDepth = subdomainDepth,
            isDeepSubdomain = isDeepSubdomain,
            hasUnusualPort = hasUnusualPort,
            isVeryLongUrl = isVeryLongUrl,
            brandImpersonated = brandImpersonated,
            hasSuspiciousStructure = hasSuspiciousStructure
        )
    }
}
