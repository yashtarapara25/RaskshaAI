package com.rakshaai.security.domain.url

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

enum class ThreatIntelStatus {
    NOT_STARTED,
    LOADING,
    READY,
    FAILED
}

object ThreatIntelligenceStore {

    private val phishingUrls = HashSet<String>(75000)
    private val phishingDomains = HashSet<String>(40000)
    private var isLoaded = false
    private var _status = ThreatIntelStatus.NOT_STARTED
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Exact trusted foundational infrastructure and global domains to prevent crowd-sourced dataset false positives.
    // NOTE: Uses exact trusted host/domain semantics; does NOT automatically trust arbitrary subdomains (e.g. evil.google.com).
    private val TRUSTED_INFRASTRUCTURE_DOMAINS = setOf(
        "google.com", "www.google.com", "accounts.google.com", "support.google.com", "mail.google.com",
        "google.co.in", "google.co.uk", "google.ca", "google.com.au",
        "googleapis.com", "gstatic.com", "googleusercontent.com",
        "github.com", "www.github.com", "github.io", "githubusercontent.com",
        "amazon.com", "www.amazon.com", "amazon.in", "amazon.co.uk",
        "wikipedia.org", "www.wikipedia.org", "wikimedia.org",
        "microsoft.com", "www.microsoft.com", "microsoftonline.com", "live.com", "outlook.com", "office.com", "windows.com",
        "apple.com", "www.apple.com", "icloud.com",
        "cloudflare.com",
        "facebook.com", "www.facebook.com", "instagram.com", "whatsapp.com", "fb.com",
        "twitter.com", "x.com",
        "youtube.com", "www.youtube.com", "youtu.be",
        "netflix.com",
        "adobe.com",
        "yahoo.com", "www.yahoo.com",
        "telegram.org", "t.me",
        "linkedin.com", "www.linkedin.com",
        "paypal.com", "www.paypal.com"
    )

    val status: ThreatIntelStatus
        get() = _status

    fun preloadAsync(context: Context) {
        if (_status == ThreatIntelStatus.READY || _status == ThreatIntelStatus.LOADING) return
        scope.launch {
            initialize(context)
        }
    }

    suspend fun initialize(context: Context) {
        mutex.withLock {
            if (_status == ThreatIntelStatus.READY || _status == ThreatIntelStatus.LOADING) return
            _status = ThreatIntelStatus.LOADING
        }

        try {
            val urls = HashSet<String>(75000)
            val domains = HashSet<String>(40000)
            var success = false

            // Load phishing URLs safely line-by-line
            try {
                context.assets.open("threat_intel/phishing_urls.txt").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
                        lines.forEach { line ->
                            val url = line.trim()
                            if (url.isNotEmpty()) {
                                urls.add(url.lowercase(Locale.ROOT))
                            }
                        }
                    }
                }
                success = true
            } catch (_: Throwable) {
                // Ignore individual asset load failure
            }

            // Load phishing domains safely line-by-line
            try {
                context.assets.open("threat_intel/phishing_domains.txt").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
                        lines.forEach { line ->
                            val domain = line.trim().lowercase(Locale.ROOT)
                            if (domain.isNotEmpty()) {
                                domains.add(domain)
                            }
                        }
                    }
                }
                success = true
            } catch (_: Throwable) {
                // Ignore individual asset load failure
            }

            mutex.withLock {
                phishingUrls.clear()
                phishingUrls.addAll(urls)
                phishingDomains.clear()
                phishingDomains.addAll(domains)
                _status = if (success || phishingUrls.isNotEmpty() || phishingDomains.isNotEmpty()) {
                    ThreatIntelStatus.READY
                } else {
                    ThreatIntelStatus.FAILED
                }
                isLoaded = _status == ThreatIntelStatus.READY
            }
        } catch (_: Throwable) {
            mutex.withLock {
                _status = ThreatIntelStatus.FAILED
                isLoaded = false
            }
        }
    }

    fun lookup(parsedUrl: ParsedUrl): ThreatIntelResult {
        if (!isLoaded || _status != ThreatIntelStatus.READY) {
            return ThreatIntelResult(
                matchLevel = ThreatMatchLevel.NO_MATCH,
                isMatched = false,
                matchedItem = null,
                detailMessage = when (_status) {
                    ThreatIntelStatus.LOADING -> "Threat intelligence database is still loading."
                    ThreatIntelStatus.FAILED -> "Threat intelligence database failed to load."
                    else -> "Threat intelligence database not loaded."
                }
            )
        }

        val normUrl = parsedUrl.normalizedUrl.lowercase(Locale.ROOT)
        if (phishingUrls.contains(normUrl)) {
            return ThreatIntelResult(
                matchLevel = ThreatMatchLevel.EXACT_URL_MATCH,
                isMatched = true,
                matchedItem = normUrl,
                detailMessage = "Exact match found in PhishTank local threat intelligence database."
            )
        }

        var host = parsedUrl.host.lowercase(Locale.ROOT)
        if (host.startsWith("www.")) {
            host = host.substring(4)
        }

        // Prevent false positives on foundational infrastructure using EXACT trusted host/domain matching.
        // NOTE: Does NOT automatically trust arbitrary subdomains (e.g. evil.google.com).
        if (TRUSTED_INFRASTRUCTURE_DOMAINS.contains(host)) {
            return ThreatIntelResult(
                matchLevel = ThreatMatchLevel.NO_MATCH,
                isMatched = false,
                matchedItem = null,
                detailMessage = "Trusted foundational domain."
            )
        }

        if (phishingDomains.contains(host)) {
            return ThreatIntelResult(
                matchLevel = ThreatMatchLevel.HOSTNAME_MATCH,
                isMatched = true,
                matchedItem = host,
                detailMessage = "Host domain '$host' matched PhishTank local threat intelligence database."
            )
        }

        return ThreatIntelResult(
            matchLevel = ThreatMatchLevel.NO_MATCH,
            isMatched = false,
            matchedItem = null,
            detailMessage = "Not found in local phishing database."
        )
    }
}
