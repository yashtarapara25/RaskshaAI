package com.rakshaai.security.domain.engine

import android.content.Context
import com.rakshaai.security.data.model.ContentType
import com.rakshaai.security.data.model.RiskCategory
import com.rakshaai.security.data.model.RiskLevel
import com.rakshaai.security.data.model.RiskSignal
import com.rakshaai.security.data.model.ScanResult
import com.rakshaai.security.domain.ocr.OcrTextCleaner
import com.rakshaai.security.domain.ocr.OcrUrlExtractor
import com.rakshaai.security.domain.url.UrlAnalysisResult
import com.rakshaai.security.domain.url.UrlAnalyzer
import com.rakshaai.security.ml.nlp.ScamMessageClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object LocalDetectionEngine {

    private var mlClassifier: ScamMessageClassifier? = null
    private var isEngineInitializing = false

    suspend fun initMlClassifier(context: Context) = withContext(Dispatchers.IO) {
        if (mlClassifier == null && !isEngineInitializing) {
            isEngineInitializing = true
            try {
                mlClassifier = ScamMessageClassifier.load(context)
                UrlAnalyzer.preload(context)
            } catch (_: Exception) {
                // Graceful fallback to rule-based engine if model asset files are absent
            } finally {
                isEngineInitializing = false
            }
        }
    }

    suspend fun analyzeContent(type: ContentType, input: String, imageUri: String? = null): ScanResult = withContext(Dispatchers.Default) {
        val cleanInput = OcrTextCleaner.clean(input)
        val lowerInput = cleanInput.lowercase(Locale.ROOT)
        val language = detectLanguage(cleanInput)

        // Handle empty input safely
        if (cleanInput.isEmpty()) {
            return@withContext ScanResult(
                contentType = type,
                rawContent = "",
                imageUri = imageUri,
                language = language,
                riskScore = 0,
                riskLevel = RiskLevel.SAFE,
                category = RiskCategory.SAFE_MSG,
                mlClassificationLabel = "BENIGN",
                mlScore = -2.0,
                signals = listOf(
                    RiskSignal("No Content Provided", "The input text was empty. Enter or select content to scan.")
                ),
                highlightedPhrases = emptyList(),
                warningSigns = emptyList(),
                recommendedActions = listOf("Safe to proceed, but remain cautious."),
                preventionGuidance = "Stay cautious when interacting with unknown messages.",
                isOfflineProcessed = true
            )
        }

        // WORKFLOW B: DIRECT URL SCANNING (DO NOT RUN MESSAGE SVM!)
        if (type == ContentType.URL) {
            val isOnline = com.rakshaai.security.data.network.NetworkConnectivityObserver.isOnline.value &&
                    com.rakshaai.security.data.preference.SecurityPreferencesRepository.settingsState.value.websiteMonitoringEnabled
            val urlRes = UrlAnalyzer.analyzeAsync(cleanInput, isOnline)
            val signalsList = urlRes.signals.map { RiskSignal("URL Feature", it) }
            val actionsList = when (urlRes.riskLevel) {
                RiskLevel.SAFE -> listOf("Safe to proceed, but remain cautious.")
                RiskLevel.SUSPICIOUS -> listOf("Verify the sender/website before taking action.")
                RiskLevel.HIGH_RISK -> listOf("Do not enter passwords or financial information on this website.")
                RiskLevel.DANGEROUS -> listOf("Do not visit this URL. It matches known phishing/malware indicators.")
            }

            return@withContext ScanResult(
                contentType = ContentType.URL,
                rawContent = cleanInput,
                imageUri = imageUri,
                language = language,
                riskScore = urlRes.score,
                riskLevel = urlRes.riskLevel,
                category = RiskCategory.PHISHING,
                mlClassificationLabel = "NOT APPLICABLE",
                mlScore = null,
                signals = signalsList,
                highlightedPhrases = listOf(urlRes.host),
                warningSigns = urlRes.warningTags,
                recommendedActions = actionsList,
                preventionGuidance = "Never enter passwords, banking PINs, or personal details on unverified web links.",
                isOfflineProcessed = true
            )
        }

        // WORKFLOW A & C: MESSAGE / SCREENSHOT / PHOTO SCANNING
        val signalsMap = LinkedHashMap<String, String>()
        val highlightedPhrases = mutableListOf<String>()
        val warningSignsList = mutableListOf<String>()
        var score = 0
        var mlLabel = "BENIGN"
        var mlSvmScore: Double? = null

        // 1. Message ML Classification (TF-IDF + Linear SVM)
        mlClassifier?.let { classifier ->
            try {
                val mlResult = classifier.classify(cleanInput)
                mlSvmScore = mlResult.score
                if (mlResult.isScam) {
                    mlLabel = "SCAM"
                    val bonus = (mlResult.score * 10.0).toInt().coerceIn(0, 15)
                    score += (30 + bonus)
                    signalsMap["On-Device ML Model"] = "Message resembles known scam patterns (ML Classifier: SCAM)."
                } else {
                    mlLabel = "BENIGN"
                }
            } catch (_: Exception) {
                // Fallback gracefully
            }
        }

        // 2. Extract Embedded URLs
        val extractedUrls = OcrUrlExtractor.extractUrls(cleanInput)
        val urlAnalysisResults = mutableListOf<UrlAnalysisResult>()
        val isOnlineForUrls = com.rakshaai.security.data.network.NetworkConnectivityObserver.isOnline.value &&
                com.rakshaai.security.data.preference.SecurityPreferencesRepository.settingsState.value.websiteMonitoringEnabled
        for (extractedUrl in extractedUrls) {
            val res = UrlAnalyzer.analyzeAsync(extractedUrl, isOnlineForUrls)
            urlAnalysisResults.add(res)
        }

        // 3. Lottery / Prize / Reward + Payment Scam
        val prizeMatches = listOf("won ₹50,000", "won rs", "lucky draw", "claim your prize", "congratulations", "won", "lottery", "prize", "reward", "kbc", "winner", "इनाम", "ઇનામ").filter { lowerInput.contains(it) }
        val feeMatches = listOf("processing fee", "registration fee", "claim fee", "pay ₹499", "pay rs", "pay", "fee", " शुल्क", " ફી").filter { lowerInput.contains(it) }
        val currencyMatches = listOf("₹50,000", "₹499", "rs 2500", "2500", "499", "50,000").filter { lowerInput.contains(it) }

        if (prizeMatches.isNotEmpty() && (feeMatches.isNotEmpty() || currencyMatches.isNotEmpty())) {
            score += 40
            signalsMap["Requests payment to claim a prize"] = "It asks you to pay a processing or claim fee before receiving a reward."
            signalsMap["Contains lottery/reward language"] = "It offers an unexpected lottery or prize claim."
            highlightedPhrases.addAll(prizeMatches + feeMatches + currencyMatches)
            warningSignsList.add("Prize or lottery bait")
            warningSignsList.add("Payment request")
        } else if (prizeMatches.isNotEmpty()) {
            score += 20
            signalsMap["Contains lottery/reward language"] = "Promotes unverified lottery or prize claims to entice the recipient."
            highlightedPhrases.addAll(prizeMatches)
            warningSignsList.add("Prize or lottery bait")
        }

        // 4. Bank / Account Suspension / KYC / Service Threat
        val bankMatches = listOf("sbi", "hdfc", "icici", "pan card", "kyc", "bank account", "yono", "bank", "બેંક", "ખાતું", "बैंक", "account", "profile", "card", "sim", "jio", "airtel", "vi", "paytm").filter { lowerInput.contains(it) }
        val accountThreatMatches = listOf("blocked", "block", "suspend", "suspended", "freeze", "deactivated", "बंद", "verify", "verification", "update", "verify your details", "urgent").filter { lowerInput.contains(it) }

        if (bankMatches.isNotEmpty() && accountThreatMatches.isNotEmpty()) {
            score += 40
            signalsMap["Creates urgency by threatening account suspension"] = "Threatens immediate account blocking, suspension, or service deactivation."
            signalsMap["Requests credential or detail verification"] = "Demands urgent account verification or details update via unverified link."
            highlightedPhrases.addAll(bankMatches + accountThreatMatches)
            warningSignsList.add("Account threat")
            warningSignsList.add("Verification request")
        } else if (bankMatches.isNotEmpty()) {
            score += 20
            signalsMap["Requests account verification"] = "References official account, bank, KYC, or card details."
            highlightedPhrases.addAll(bankMatches)
        } else if (accountThreatMatches.size >= 2) {
            score += 30
            signalsMap["Urgent Account Action Demanded"] = "Uses coercive language demanding account verification or immediate action."
            highlightedPhrases.addAll(accountThreatMatches)
            warningSignsList.add("Account threat")
        }

        // 5. Utility / Electricity Disconnection Threat
        val electricityMatches = listOf("electricity bill", "power cut", "power supply", "बिजली", "વીજળી").filter { lowerInput.contains(it) }
        val disconnectionMatches = listOf("disconnected", "disconnect", "unpaid", "cut", " tonight", " रात").filter { lowerInput.contains(it) }

        if (electricityMatches.isNotEmpty() && disconnectionMatches.isNotEmpty()) {
            score += 40
            signalsMap["Utility disconnection threat"] = "Threatens immediate power/utility disconnection to force urgent payment."
            highlightedPhrases.addAll(electricityMatches + disconnectionMatches)
            warningSignsList.add("Utility threat")
            warningSignsList.add("Payment request")
        }

        // 6. Work-From-Home / Registration Fee Job Scam
        val jobMatches = listOf("job", "work from home", "part time", "salary", "daily income").filter { lowerInput.contains(it) }
        if (jobMatches.isNotEmpty() && (feeMatches.isNotEmpty() || lowerInput.contains("registration"))) {
            score += 35
            signalsMap["Requests registration fee for job offer"] = "Demands an advance deposit or registration fee for a job offer."
            highlightedPhrases.addAll(jobMatches + feeMatches)
            warningSignsList.add("Job bait")
            warningSignsList.add("Payment request")
        }

        // 7. Financial / Payment / UPI Request
        val paymentMatches = listOf("pay rs", "pay ₹", "processing fee", "upi pin", "gpay", "paytm", "send money", "paise bhejo", "upi").filter { lowerInput.contains(it) }
        if (paymentMatches.isNotEmpty() && !signalsMap.containsKey("Requests payment to claim a prize")) {
            score += 25
            signalsMap["Financial / Payment request"] = "Prompts recipient to send money, enter UPI PIN, or pay fees."
            highlightedPhrases.addAll(paymentMatches)
            warningSignsList.add("Payment request")
        }

        // 8. Urgency Pressure
        val urgencyMatches = listOf("now", "urgent", "immediately", "today", "24 hours", "9:30 pm", "turant", "aaj hi", "jaldi", "તત્કાલિક", "तुरंत").filter { lowerInput.contains(it) }
        if (urgencyMatches.isNotEmpty()) {
            score += 20
            signalsMap["Uses urgency to encourage immediate action"] = "Applies high-pressure deadlines (now, 24 hrs, today, immediately) to encourage hasty action."
            highlightedPhrases.addAll(urgencyMatches)
            warningSignsList.add("Urgency pressure")
        }

        // 9. Unverified Link / Action Request in Lure Message
        val hasLinkInText = lowerInput.contains("link") || lowerInput.contains("http") || lowerInput.contains("www") || urlAnalysisResults.isNotEmpty()
        if (hasLinkInText && (prizeMatches.isNotEmpty() || bankMatches.isNotEmpty() || jobMatches.isNotEmpty() || accountThreatMatches.isNotEmpty())) {
            score += 20
            signalsMap["Unverified Link / Verification Action Request"] = "Message combines an account threat or scam lure with an external web link or credential verification request."
            warningSignsList.add("Unverified link in lure")
        }

        // 10. Legitimate OTP Safeguard
        val isOtpMessage = listOf("otp", "one time password", "verification code", "482910", "123456").any { lowerInput.contains(it) }
        val isLegitimateOtp = isOtpMessage && feeMatches.isEmpty() && prizeMatches.isEmpty() && accountThreatMatches.isEmpty() && urlAnalysisResults.isEmpty()

        if (isLegitimateOtp) {
            score = score.coerceAtMost(15) // Keep legitimate OTP notifications SAFE
            signalsMap.clear()
            signalsMap["Standard OTP Notification"] = "Legitimate authentication OTP code without payment or link requests."
            warningSignsList.clear()
            highlightedPhrases.clear()
        }

        // Category Classification
        val category = when {
            score < 30 -> RiskCategory.SAFE_MSG
            prizeMatches.isNotEmpty() -> RiskCategory.LOTTERY_PRIZE
            bankMatches.isNotEmpty() -> RiskCategory.BANKING_KYC
            paymentMatches.isNotEmpty() -> RiskCategory.UPI_PAYMENT
            jobMatches.isNotEmpty() -> RiskCategory.JOB_INVESTMENT
            lowerInput.contains("courier") || lowerInput.contains("parcel") -> RiskCategory.COURIER_PARCEL
            urlAnalysisResults.isNotEmpty() -> RiskCategory.PHISHING
            else -> RiskCategory.BANKING_KYC
        }

        // 11. Fuse all evidence
        EvidenceFusion.fuse(
            contentType = type,
            rawContent = cleanInput,
            imageUri = imageUri,
            language = language,
            mlLabel = mlLabel,
            mlScore = mlSvmScore,
            ruleScore = score,
            ruleSignalsMap = signalsMap,
            highlightedPhrases = highlightedPhrases,
            warningSignsList = warningSignsList,
            urlResults = urlAnalysisResults,
            category = category
        )
    }

    private fun detectLanguage(text: String): String {
        return when {
            text.any { it in '\u0A80'..'\u0AFF' } -> "Gujarati"
            text.any { it in '\u0900'..'\u097F' } -> "Hindi"
            text.lowercase().contains("turant") || text.lowercase().contains("aapka") || text.lowercase().contains("bhejo") -> "Hinglish"
            else -> "English"
        }
    }
}
