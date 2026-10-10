package com.rakshaai.security

import com.rakshaai.security.data.model.ContentType
import com.rakshaai.security.data.model.RiskLevel
import com.rakshaai.security.domain.engine.LocalDetectionEngine
import com.rakshaai.security.domain.ocr.OcrTextCleaner
import com.rakshaai.security.domain.ocr.OcrUrlExtractor
import com.rakshaai.security.domain.url.ParsedUrl
import com.rakshaai.security.domain.url.ThreatIntelResult
import com.rakshaai.security.domain.url.ThreatMatchLevel
import com.rakshaai.security.domain.url.UrlAnalyzer
import com.rakshaai.security.domain.url.UrlFeatureExtractor
import com.rakshaai.security.domain.url.UrlNormalizer
import com.rakshaai.security.domain.url.UrlParser
import com.rakshaai.security.domain.url.UrlRuleEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlAnalyzerTest {

    @Test
    fun test01_NormalUrl_IsSafe() {
        val res = UrlAnalyzer.analyze("https://example.com")
        assertEquals(RiskLevel.SAFE, res.riskLevel)
        assertTrue(res.score < 30)
    }

    @Test
    fun test02_EasternMirrorLotteryNewsUrl_IsNotFalselyHighRisk() {
        val newsUrl = "https://easternmirrornagaland.com/nagaland-state-lottery-sambad-latest-results-feb-15-8-pm-winners-of-dear-flamingo-evening-declared"
        val res = UrlAnalyzer.analyze(newsUrl)
        
        // Must NOT become HIGH_RISK or DANGEROUS merely because "lottery" or "winner" is in path
        assertTrue(
            "Eastern Mirror news URL must be SAFE or low risk, but was ${res.riskLevel} (score: ${res.score})",
            res.riskLevel == RiskLevel.SAFE
        )
        assertTrue(res.score < 30)
    }

    @Test
    fun test03_IpAddressUrl_TriggersRawIpWarning() {
        val res = UrlAnalyzer.analyze("http://192.168.1.1/login.php")
        assertTrue(res.features.isIpAddress)
        assertTrue(res.score >= 25)
        assertTrue(res.warningTags.contains("Raw IP Host"))
    }

    @Test
    fun test04_UserInfoAtUrl_TriggersUserInfoWarning() {
        val res = UrlAnalyzer.analyze("http://admin:secret@example.com/auth")
        assertTrue(res.features.hasUserInfo)
        assertTrue(res.score >= 25)
        assertTrue(res.warningTags.contains("User-Info Redirect"))
    }

    @Test
    fun test05_PunycodeUrl_TriggersPunycodeWarning() {
        val res = UrlAnalyzer.analyze("http://xn--sbi-801a.com/verify")
        assertTrue(res.features.isPunycode)
        assertTrue(res.score >= 20)
        assertTrue(res.warningTags.contains("Punycode Homograph"))
    }

    @Test
    fun test06_ShortenerUrl_TriggersShortenerWarning() {
        val res = UrlAnalyzer.analyze("https://bit.ly/3xXyZ")
        assertTrue(res.features.isShortener)
        assertTrue(res.warningTags.contains("URL Shortener"))
    }

    @Test
    fun test07_SuspiciousTldUrl_TriggersSuspiciousTldWarning() {
        val res = UrlAnalyzer.analyze("http://secure-bank-login.xyz")
        assertTrue(res.features.isSuspiciousTld)
        assertTrue(res.warningTags.contains("Suspicious TLD"))
    }

    @Test
    fun test08_DeepSubdomainUrl_TriggersDeepSubdomainWarning() {
        val res = UrlAnalyzer.analyze("http://a.b.c.d.example.com/login")
        assertTrue(res.features.isDeepSubdomain)
        assertTrue(res.warningTags.contains("Deep Subdomain"))
    }

    @Test
    fun test09_InsecureHttpUrl_TriggersInsecureHttpWarning() {
        val res = UrlAnalyzer.analyze("http://example.com")
        assertTrue(res.features.isHttp)
        assertFalse(res.features.isHttps)
        assertTrue(res.warningTags.contains("Insecure HTTP"))
    }

    @Test
    fun test10_BrandImpersonationUrl_TriggersBrandImpersonationWarning() {
        val res = UrlAnalyzer.analyze("https://sbi-login-security.xyz/verify")
        assertEquals("SBI", res.features.brandImpersonated)
        assertTrue(res.score >= 30)
        assertTrue(res.warningTags.contains("Brand Impersonation"))
    }

    @Test
    fun test11_DirectUrlScan_DoesNotRunMessageSvm() = runBlocking {
        val directUrl = "https://example.com"
        val result = LocalDetectionEngine.analyzeContent(ContentType.URL, directUrl)
        
        assertEquals("NOT APPLICABLE", result.mlClassificationLabel)
        assertEquals(null, result.mlScore)
        assertEquals(RiskLevel.SAFE, result.riskLevel)
    }

    @Test
    fun test12_MessageContainingUrl_FusesMessageAndUrlEvidence() = runBlocking {
        val message = "Your bank account is suspended. Verify immediately at https://sbi-security-login.xyz/auth"
        val result = LocalDetectionEngine.analyzeContent(ContentType.MESSAGE, message)

        assertTrue(
            "Scam message with suspicious URL should be HIGH RISK or DANGEROUS",
            result.riskLevel == RiskLevel.HIGH_RISK || result.riskLevel == RiskLevel.DANGEROUS
        )
        assertTrue(result.riskScore >= 60)
        assertTrue(result.warningSigns.contains("Brand Impersonation") || result.warningSigns.contains("Suspicious link"))
    }

    @Test
    fun test13_MultipleUrlsExtractor_ExtractsAllUrls() {
        val text = "Check site 1: https://bit.ly/123 and site 2: http://example.xyz/test"
        val extracted = OcrUrlExtractor.extractUrls(text)

        assertEquals(2, extracted.size)
        assertTrue(extracted.contains("https://bit.ly/123"))
        assertTrue(extracted.contains("http://example.xyz/test"))
    }

    @Test
    fun test14_OcrTextCleaner_PreservesKeyTokensAndUrls() {
        val rawOcr = "Your OTP is 482910. Pay RS. 500 for KYC update at hTTp://sbi-bank.xyz"
        val cleaned = OcrTextCleaner.clean(rawOcr)

        assertTrue(cleaned.contains("482910"))
        assertTrue(cleaned.contains("500"))
        assertTrue(cleaned.contains("KYC"))
        assertTrue(cleaned.contains("https://sbi-bank.xyz") || cleaned.contains("http://sbi-bank.xyz"))
    }

    @Test
    fun test15_ThreatIntelRuleScoring_KnownPhishingUrlMatch() {
        val parsed = UrlParser.parse("https://allegrolokalnie.lokalna-6747.sbs")
        val features = UrlFeatureExtractor.extract(parsed)
        val threatIntel = ThreatIntelResult(
            matchLevel = ThreatMatchLevel.EXACT_URL_MATCH,
            isMatched = true,
            matchedItem = parsed.normalizedUrl,
            detailMessage = "Matched known phishing URL"
        )

        val res = UrlRuleEngine.evaluate(parsed, features, threatIntel)
        assertTrue(res.score >= 60)
        assertTrue(res.riskLevel == RiskLevel.HIGH_RISK || res.riskLevel == RiskLevel.DANGEROUS)
    }

    @Test
    fun test16_ScreenshotOcrWorkflow_SeparatesTextAndUrls() = runBlocking {
        val ocrText = "Congratulations! You won ₹50,000 in lucky draw. Claim at https://bit.ly/claim-prize"
        val result = LocalDetectionEngine.analyzeContent(ContentType.SCREENSHOT, ocrText)

        assertTrue(
            "Screenshot scam with shortener should be HIGH RISK or DANGEROUS",
            result.riskLevel == RiskLevel.HIGH_RISK || result.riskLevel == RiskLevel.DANGEROUS
        )
        assertTrue(result.riskScore >= 60)
    }

    @Test
    fun test17_TrustedDomains_AreSafeAndDoNotMatchPhishtank() {
        val trustedUrls = listOf(
            "https://google.com",
            "https://www.google.com",
            "https://accounts.google.com",
            "https://support.google.com",
            "https://mail.google.com",
            "https://github.com",
            "https://amazon.com",
            "https://wikipedia.org",
            "https://microsoft.com",
            "https://login.microsoftonline.com"
        )
        for (url in trustedUrls) {
            val res = UrlAnalyzer.analyze(url)
            assertEquals("Trusted URL $url should be SAFE", RiskLevel.SAFE, res.riskLevel)
            assertTrue("Trusted URL $url score should be < 30", res.score < 30)
            assertFalse("Trusted URL $url must not match PhishTank domain", res.warningTags.contains("PhishTank Domain Match"))
        }
    }

    @Test
    fun test18_UntrustedSubdomains_DoNotBypassPhishtank() {
        val untrustedUrls = listOf(
            "https://evil.google.com",
            "https://malicious.github.com"
        )
        for (url in untrustedUrls) {
            val res = UrlAnalyzer.analyze(url)
            assertFalse("Untrusted subdomain $url must not be short-circuited as a trusted foundational domain", res.threatIntelResult.detailMessage.contains("Trusted foundational domain"))
        }
    }
}
