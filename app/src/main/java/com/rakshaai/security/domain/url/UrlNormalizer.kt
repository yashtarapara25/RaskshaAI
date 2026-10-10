package com.rakshaai.security.domain.url

import java.util.Locale

object UrlNormalizer {

    fun normalize(rawInput: String): String {
        var url = rawInput.trim()
            .replace("\r", "")
            .replace("\n", "")
            .replace("\t", "")

        // Remove surrounding quotes or angle brackets
        if ((url.startsWith("<") && url.endsWith(">")) ||
            (url.startsWith("\"") && url.endsWith("\"")) ||
            (url.startsWith("'") && url.endsWith("'"))
        ) {
            url = url.substring(1, url.length - 1).trim()
        }

        // Remove trailing sentence punctuation
        url = url.replace(Regex("""[\.\,\)\}\]\"\';\!]+$"""), "")

        if (url.isEmpty()) return ""

        // If missing scheme, prepend http:// for consistent processing
        if (!url.startsWith("http://", ignoreCase = true) &&
            !url.startsWith("https://", ignoreCase = true)
        ) {
            url = "http://$url"
        }

        // Separate host and path/query to lowercase host
        return try {
            val schemeEnd = url.indexOf("://")
            if (schemeEnd != -1) {
                val scheme = url.substring(0, schemeEnd).lowercase(Locale.ROOT)
                val rest = url.substring(schemeEnd + 3)
                val pathStart = rest.indexOf('/')
                if (pathStart != -1) {
                    val host = rest.substring(0, pathStart).lowercase(Locale.ROOT)
                    val pathAndQuery = rest.substring(pathStart)
                    "$scheme://$host$pathAndQuery"
                } else {
                    val host = rest.lowercase(Locale.ROOT)
                    "$scheme://$host"
                }
            } else {
                url.lowercase(Locale.ROOT)
            }
        } catch (_: Exception) {
            url
        }
    }
}
