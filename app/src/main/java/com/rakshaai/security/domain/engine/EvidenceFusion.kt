package com.rakshaai.security.domain.engine

import com.rakshaai.security.data.model.ContentType
import com.rakshaai.security.data.model.RiskCategory
import com.rakshaai.security.data.model.RiskLevel
import com.rakshaai.security.data.model.RiskSignal
import com.rakshaai.security.data.model.ScanResult
import com.rakshaai.security.domain.url.UrlAnalysisResult

object EvidenceFusion {

    fun fuse(
        contentType: ContentType,
        rawContent: String,
        imageUri: String?,
        language: String,
        mlLabel: String,
        mlScore: Double?,
        ruleScore: Int,
        ruleSignalsMap: LinkedHashMap<String, String>,
        highlightedPhrases: MutableList<String>,
        warningSignsList: MutableList<String>,
        urlResults: List<UrlAnalysisResult>,
        category: RiskCategory
    ): ScanResult {
        var totalScore = ruleScore
        val fusedSignals = LinkedHashMap<String, String>(ruleSignalsMap)

        // Fuse URL Analyzer results
        for (urlRes in urlResults) {
            totalScore += urlRes.score
            warningSignsList.addAll(urlRes.warningTags)
            highlightedPhrases.add(urlRes.host)

            if (urlRes.threatIntelResult.isMatched) {
                fusedSignals["PhishTank Match (${urlRes.host})"] = urlRes.threatIntelResult.detailMessage
            } else {
                for (sig in urlRes.signals) {
                    fusedSignals["URL Signal (${urlRes.host})"] = sig
                }
            }
        }

        val finalScore = totalScore.coerceIn(0, 100)

        val riskLevel = when {
            finalScore >= 80 -> RiskLevel.DANGEROUS
            finalScore >= 60 -> RiskLevel.HIGH_RISK
            finalScore >= 30 -> RiskLevel.SUSPICIOUS
            else -> RiskLevel.SAFE
        }

        val preventionGuidance = when (category) {
            RiskCategory.LOTTERY_PRIZE -> "You never have to pay money or processing fees to receive money or prizes you have won."
            RiskCategory.BANKING_KYC -> "Verify KYC requests only through your bank's official app or website."
            RiskCategory.JOB_INVESTMENT -> "Do not pay registration or processing fees for a job offer."
            RiskCategory.UPI_PAYMENT -> "Entering a UPI PIN is only required to SEND money, never to receive cashback or payments."
            RiskCategory.PHISHING -> "Never enter credentials, passwords, or personal details on unverified web links."
            else -> "Stay cautious when receiving unsolicited messages asking for urgent action."
        }

        val recommendedActions = when (riskLevel) {
            RiskLevel.SAFE -> listOf("Safe to proceed, but remain cautious.")
            RiskLevel.SUSPICIOUS -> listOf("Verify the sender before taking action.")
            RiskLevel.HIGH_RISK -> listOf("Do not click links or send money. Verify through the official source.")
            RiskLevel.DANGEROUS -> listOf("Do not interact with this message. Do not send money, credentials, OTPs, or personal information.")
        }

        val signalsList = if (fusedSignals.isEmpty()) {
            listOf(RiskSignal("No major threat signals detected", "No suspicious urgency, malicious links, or credential requests were found."))
        } else {
            fusedSignals.map { (title, desc) -> RiskSignal(title, desc) }
        }

        return ScanResult(
            contentType = contentType,
            rawContent = rawContent,
            imageUri = imageUri,
            language = language,
            riskScore = finalScore,
            riskLevel = riskLevel,
            category = category,
            mlClassificationLabel = mlLabel,
            mlScore = mlScore,
            signals = signalsList,
            highlightedPhrases = highlightedPhrases.distinct(),
            warningSigns = warningSignsList.distinct(),
            recommendedActions = recommendedActions,
            preventionGuidance = preventionGuidance,
            isOfflineProcessed = true
        )
    }
}
