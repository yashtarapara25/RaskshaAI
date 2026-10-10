package com.rakshaai.security.domain.url

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

interface ThreatIntelProvider {
    val providerName: String
    suspend fun checkUrlReputation(parsedUrl: ParsedUrl): ThreatIntelResult
}

class URLhausProvider : ThreatIntelProvider {
    override val providerName: String = "URLhaus Malware Intelligence"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    override suspend fun checkUrlReputation(parsedUrl: ParsedUrl): ThreatIntelResult = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("url", parsedUrl.normalizedUrl)
                .build()

            val request = Request.Builder()
                .url("https://urlhaus-api.abuse.ch/v1/url/")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ThreatIntelResult(
                        matchLevel = ThreatMatchLevel.NO_MATCH,
                        isMatched = false,
                        matchedItem = null,
                        detailMessage = "URLhaus provider HTTP ${response.code}"
                    )
                }

                val responseBody = response.body?.string() ?: ""
                if (responseBody.contains("\"query_status\": \"ok\"") && responseBody.contains("\"threat\":")) {
                    return@withContext ThreatIntelResult(
                        matchLevel = ThreatMatchLevel.EXACT_URL_MATCH,
                        isMatched = true,
                        matchedItem = parsedUrl.normalizedUrl,
                        detailMessage = "Flagged as active malware URL in URLhaus threat database."
                    )
                }
            }

            ThreatIntelResult(
                matchLevel = ThreatMatchLevel.NO_MATCH,
                isMatched = false,
                matchedItem = null,
                detailMessage = "No match found in URLhaus database."
            )
        } catch (e: Throwable) {
            ThreatIntelResult(
                matchLevel = ThreatMatchLevel.NO_MATCH,
                isMatched = false,
                matchedItem = null,
                detailMessage = "URLhaus query error: ${e.localizedMessage ?: "Network error"}"
            )
        }
    }
}

class CompositeThreatIntelProvider(
    private val onlineProviders: List<ThreatIntelProvider> = listOf(URLhausProvider())
) {
    suspend fun checkUrl(parsedUrl: ParsedUrl, isOnline: Boolean): ThreatIntelResult {
        // 1. Local Threat Intelligence lookup
        val localResult = ThreatIntelligenceStore.lookup(parsedUrl)
        if (localResult.isMatched) {
            return localResult
        }

        // 2. Online Threat Intelligence lookup if internet is available
        if (isOnline) {
            for (provider in onlineProviders) {
                val onlineResult = provider.checkUrlReputation(parsedUrl)
                if (onlineResult.isMatched) {
                    return onlineResult
                }
            }
        }

        return localResult
    }
}
