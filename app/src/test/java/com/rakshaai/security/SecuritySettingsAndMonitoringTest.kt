package com.rakshaai.security

import com.rakshaai.security.data.network.OnlineMonitoringManager
import com.rakshaai.security.data.preference.ProtectionFeatureStatus
import com.rakshaai.security.data.preference.SecurityPreferences
import com.rakshaai.security.domain.apk.ApkMonitoringManager
import com.rakshaai.security.domain.url.CompositeThreatIntelProvider
import com.rakshaai.security.domain.url.ThreatMatchLevel
import com.rakshaai.security.domain.url.URLhausProvider
import com.rakshaai.security.domain.url.UrlAnalyzer
import com.rakshaai.security.domain.url.UrlParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class SecuritySettingsAndMonitoringTest {

    @Test
    fun testPreferences_DefaultValuesAreCorrect() {
        val prefs = SecurityPreferences()
        assertTrue("Website monitoring should be enabled by default", prefs.websiteMonitoringEnabled)
        assertTrue("APK monitoring should be enabled by default", prefs.apkMonitoringEnabled)
        assertFalse("Notification monitoring should be disabled by default", prefs.notificationMonitoringEnabled)
        assertFalse("Network monitoring should be disabled by default", prefs.networkMonitoringEnabled)
        assertTrue("Automatic threat updates should be enabled by default", prefs.automaticThreatUpdatesEnabled)
        assertTrue("Security alerts should be enabled by default", prefs.securityAlertsEnabled)
    }

    @Test
    fun testPreferences_ToggleIsolation() {
        val prefs = SecurityPreferences(
            websiteMonitoringEnabled = true,
            apkMonitoringEnabled = false,
            notificationMonitoringEnabled = true,
            networkMonitoringEnabled = false
        )
        assertTrue(prefs.websiteMonitoringEnabled)
        assertFalse(prefs.apkMonitoringEnabled)
        assertTrue(prefs.notificationMonitoringEnabled)
        assertFalse(prefs.networkMonitoringEnabled)
    }

    @Test
    fun testWebsiteFeatureStatus_OnlineAndEnabled_ReturnsActive() {
        val state = OnlineMonitoringManager.getWebsiteFeatureState(isOnline = true, isEnabled = true)
        assertEquals(ProtectionFeatureStatus.ACTIVE, state.status)
        assertTrue(state.isRuntimeActive)
    }

    @Test
    fun testWebsiteFeatureStatus_OfflineAndEnabled_ReturnsWaitingForInternet() {
        val state = OnlineMonitoringManager.getWebsiteFeatureState(isOnline = false, isEnabled = true)
        assertEquals(ProtectionFeatureStatus.WAITING_FOR_INTERNET, state.status)
        assertFalse(state.isRuntimeActive)
    }

    @Test
    fun testWebsiteFeatureStatus_Disabled_ReturnsDisabled() {
        val state = OnlineMonitoringManager.getWebsiteFeatureState(isOnline = true, isEnabled = false)
        assertEquals(ProtectionFeatureStatus.DISABLED, state.status)
        assertFalse(state.isRuntimeActive)
    }

    @Test
    fun testApkFeatureStatus_Enabled_ReturnsActive() {
        val state = OnlineMonitoringManager.getApkFeatureState(isEnabled = true)
        assertEquals(ProtectionFeatureStatus.ACTIVE, state.status)
        assertTrue(state.isRuntimeActive)
    }

    @Test
    fun testApkFeatureStatus_Disabled_ReturnsDisabled() {
        val state = OnlineMonitoringManager.getApkFeatureState(isEnabled = false)
        assertEquals(ProtectionFeatureStatus.DISABLED, state.status)
        assertFalse(state.isRuntimeActive)
    }

    @Test
    fun testComputeSha256_ValidStream_ComputesCorrectHash() {
        val inputData = "RakshaAI Security SHA256 Test String".toByteArray(Charsets.UTF_8)
        val stream = ByteArrayInputStream(inputData)
        val hash = ApkMonitoringManager.computeSha256(stream)
        
        assertEquals(64, hash.length)
        assertTrue(hash.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun testCompositeThreatIntelProvider_Offline_ReturnsNoMatchSafely() = runBlocking {
        val provider = CompositeThreatIntelProvider()
        val parsedUrl = UrlParser.parse("https://example.com/login")
        val result = provider.checkUrl(parsedUrl, isOnline = false)
        assertNotNull(result)
        assertEquals(ThreatMatchLevel.NO_MATCH, result.matchLevel)
        assertFalse(result.isMatched)
    }

    @Test
    fun testUrlAnalyzer_AnalyzeAsync_ExecutesWithoutCrash() = runBlocking {
        val result = UrlAnalyzer.analyzeAsync("https://example-test-site.org", isOnline = false)
        assertNotNull(result)
        assertEquals("example-test-site.org", result.host)
    }

    @Test
    fun testURLhausProvider_HandlesInvalidDomainGracefully() = runBlocking {
        val provider = URLhausProvider()
        val parsedUrl = UrlParser.parse("https://invalid-nonexistent-domain-12345.org")
        val result = provider.checkUrlReputation(parsedUrl)
        assertNotNull(result)
        assertFalse(result.isMatched)
    }
}
