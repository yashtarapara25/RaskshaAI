package com.rakshaai.security

import com.rakshaai.security.data.model.ContentType
import com.rakshaai.security.data.model.RiskLevel
import com.rakshaai.security.domain.engine.LocalDetectionEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskEngineTest {

    @Test
    fun test1_LotteryPaymentScam_ReturnsHighRiskOrDangerous() = runBlocking {
        val message = "Congratulations! You have won ₹50,000 in our lucky draw. Pay ₹499 processing fee now to claim your prize."
        val result = LocalDetectionEngine.analyzeContent(ContentType.MESSAGE, message)
        
        assertTrue(
            "Lottery scam should be HIGH RISK or DANGEROUS, but was ${result.riskLevel}",
            result.riskLevel == RiskLevel.HIGH_RISK || result.riskLevel == RiskLevel.DANGEROUS
        )
        assertTrue(result.riskScore >= 60)
        assertTrue(result.signals.any { it.title.contains("prize", ignoreCase = true) || it.title.contains("payment", ignoreCase = true) })
    }

    @Test
    fun test2_BankKycUrgentThreat_ReturnsHighRiskOrDangerous() = runBlocking {
        val message = "Your bank account will be blocked today. Complete KYC immediately using this link: http://sbi-update.xyz"
        val result = LocalDetectionEngine.analyzeContent(ContentType.MESSAGE, message)

        assertTrue(
            "Bank KYC threat should be HIGH RISK or DANGEROUS, but was ${result.riskLevel}",
            result.riskLevel == RiskLevel.HIGH_RISK || result.riskLevel == RiskLevel.DANGEROUS
        )
        assertTrue(result.riskScore >= 60)
    }

    @Test
    fun test3_WorkFromHomeJobFeeScam_ReturnsHighRiskOrDangerous() = runBlocking {
        val message = "You have been selected for a work from home job. Pay ₹500 registration fee now to receive daily salary."
        val result = LocalDetectionEngine.analyzeContent(ContentType.MESSAGE, message)

        assertTrue(
            "Job scam with fee should be HIGH RISK or DANGEROUS, but was ${result.riskLevel}",
            result.riskLevel == RiskLevel.HIGH_RISK || result.riskLevel == RiskLevel.DANGEROUS
        )
        assertTrue(result.riskScore >= 60)
    }

    @Test
    fun test4_NormalDinnerConversation_ReturnsSafe() = runBlocking {
        val message = "Hey, are we still meeting for dinner tonight?"
        val result = LocalDetectionEngine.analyzeContent(ContentType.MESSAGE, message)

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertTrue(result.riskScore < 30)
    }

    @Test
    fun test5_NormalProjectMessage_ReturnsSafe() = runBlocking {
        val message = "Hi Rahul, please review the final presentation slides for tomorrow's team meeting."
        val result = LocalDetectionEngine.analyzeContent(ContentType.MESSAGE, message)

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertTrue(result.riskScore < 30)
    }

    @Test
    fun test6_LegitimateOtpNotification_ReturnsSafeOrLowRisk() = runBlocking {
        val message = "Your OTP is 482913. Do not share this OTP with anyone."
        val result = LocalDetectionEngine.analyzeContent(ContentType.MESSAGE, message)

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertTrue(result.riskScore < 30)
    }

    @Test
    fun test7_SuspiciousUrlAccountThreat_ReturnsHighRiskOrDangerous() = runBlocking {
        val message = "Your account has been suspended due to security reasons. Verify now at http://secure-icici-login.top"
        val result = LocalDetectionEngine.analyzeContent(ContentType.MESSAGE, message)

        assertTrue(
            "Suspicious URL with account threat should be HIGH RISK or DANGEROUS",
            result.riskLevel == RiskLevel.HIGH_RISK || result.riskLevel == RiskLevel.DANGEROUS
        )
        assertTrue(result.riskScore >= 60)
    }

    @Test
    fun test8_EmptyMessage_ReturnsSafeWithoutCrash() = runBlocking {
        val message = ""
        val result = LocalDetectionEngine.analyzeContent(ContentType.MESSAGE, message)

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertEquals(0, result.riskScore)
    }

    @Test
    fun test9_VeryLongMessage_ExecutesWithoutCrash() = runBlocking {
        val longMessage = "Scam alert! ".repeat(1000)
        val result = LocalDetectionEngine.analyzeContent(ContentType.MESSAGE, longMessage)

        assertTrue(result.riskScore >= 0)
    }

    @Test
    fun test10_OfflineMode_ProcessingIsOffline() = runBlocking {
        val message = "Test message for offline engine check."
        val result = LocalDetectionEngine.analyzeContent(ContentType.MESSAGE, message)

        assertTrue(result.isOfflineProcessed)
    }
}
