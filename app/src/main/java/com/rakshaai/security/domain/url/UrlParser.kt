package com.rakshaai.security.domain.url

import java.net.URI
import java.util.Locale

object UrlParser {

    fun parse(originalInput: String): ParsedUrl {
        val normalized = UrlNormalizer.normalize(originalInput)

        return try {
            val uri = URI(normalized)
            val host = (uri.host ?: extractHostFallback(normalized)).lowercase(Locale.ROOT)
            val scheme = (uri.scheme ?: "http").lowercase(Locale.ROOT)
            val port = if (uri.port != -1) uri.port else if (scheme == "https") 443 else 80
            val path = uri.path ?: "/"
            val query = uri.query
            val fragment = uri.fragment
            val userInfo = uri.userInfo
            val authority = uri.authority

            ParsedUrl(
                originalUrl = originalInput,
                normalizedUrl = normalized,
                scheme = scheme,
                host = host,
                port = port,
                path = if (path.isEmpty()) "/" else path,
                query = query,
                fragment = fragment,
                userInfo = userInfo,
                authority = authority
            )
        } catch (_: Exception) {
            val host = extractHostFallback(normalized)
            ParsedUrl(
                originalUrl = originalInput,
                normalizedUrl = normalized,
                scheme = if (normalized.startsWith("https://")) "https" else "http",
                host = host,
                port = 80,
                path = "/",
                query = null,
                fragment = null,
                userInfo = null,
                authority = host
            )
        }
    }

    private fun extractHostFallback(normalizedUrl: String): String {
        return try {
            var url = normalizedUrl
            val schemeIdx = url.indexOf("://")
            if (schemeIdx != -1) {
                url = url.substring(schemeIdx + 3)
            }
            val cutIdx = url.indexOfAny(charArrayOf('/', '?', '#', ':'))
            val rawHost = if (cutIdx != -1) url.substring(0, cutIdx) else url
            rawHost.lowercase(Locale.ROOT).trim()
        } catch (_: Exception) {
            "unknown"
        }
    }
}
