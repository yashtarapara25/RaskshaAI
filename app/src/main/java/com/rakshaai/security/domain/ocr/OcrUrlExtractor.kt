package com.rakshaai.security.domain.ocr

object OcrUrlExtractor {

    private val URL_REGEX = Regex(
        """(?i)\b(?:https?://|www\.)[a-z0-9\-\.]+\.[a-z]{2,10}(?:/[^\s]*)?"""
    )

    private val BARE_DOMAIN_REGEX = Regex(
        """(?i)\b[a-z0-9\-]+\.(?:com|in|co\.in|net|org|xyz|top|online|site|ru|info|live)(?:/[^\s]*)?"""
    )

    fun extractUrls(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val results = mutableListOf<String>()

        URL_REGEX.findAll(text).forEach { match ->
            val cleanUrl = match.value.trim().trimEnd('.', ',', ')', ']', '}')
            if (cleanUrl.isNotEmpty()) {
                results.add(cleanUrl)
            }
        }

        if (results.isEmpty()) {
            BARE_DOMAIN_REGEX.findAll(text).forEach { match ->
                val cleanUrl = match.value.trim().trimEnd('.', ',', ')', ']', '}')
                if (cleanUrl.isNotEmpty()) {
                    results.add("http://$cleanUrl")
                }
            }
        }

        return results.distinct()
    }
}
