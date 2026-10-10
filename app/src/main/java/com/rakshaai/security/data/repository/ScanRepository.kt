package com.rakshaai.security.data.repository

import android.content.Context
import com.rakshaai.security.data.local.ScanDatabase
import com.rakshaai.security.data.local.toScanEntity
import com.rakshaai.security.data.local.toScanResult
import com.rakshaai.security.data.model.ContentType
import com.rakshaai.security.data.model.RiskCategory
import com.rakshaai.security.data.model.RiskLevel
import com.rakshaai.security.data.model.RiskSignal
import com.rakshaai.security.data.model.ScanResult
import com.rakshaai.security.domain.engine.LocalDetectionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PresetSample(
    val title: String,
    val language: String,
    val contentType: ContentType,
    val text: String
)

object ScanRepository {

    private var database: ScanDatabase? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _history = MutableStateFlow<List<ScanResult>>(emptyList())
    val history: StateFlow<List<ScanResult>> = _history.asStateFlow()

    fun initialize(context: Context) {
        if (database == null) {
            val db = ScanDatabase.getDatabase(context)
            database = db
            val dao = db.scanDao()
            scope.launch {
                val existing = dao.observeAllScans().first()
                if (existing.isEmpty()) {
                    val initialRecords = listOf(
                        ScanResult(
                            contentType = ContentType.MESSAGE,
                            rawContent = "સાવધાન! તમારું SBI બેંક ખાતું 24 કલાકમાં ફ્રીઝ થઈ જશે. અત્યારે જ KYC અપડેટ કરો: http://sbi-verify.kcy-update.xyz",
                            language = "Gujarati",
                            riskScore = 96,
                            riskLevel = RiskLevel.HIGH_RISK,
                            category = RiskCategory.BANKING_KYC,
                            signals = listOf(
                                RiskSignal("Artificial Urgency", "Mentions 24-hour account suspension in Gujarati."),
                                RiskSignal("Banking Impersonation", "Impersonates State Bank of India (SBI)."),
                                RiskSignal("Malicious Domain", "Uses unverified .xyz phishing link.")
                            ),
                            recommendedActions = listOf(
                                "Do NOT click the .xyz link.",
                                "Verify your account status directly via SBI YONO App."
                            )
                        ),
                        ScanResult(
                            contentType = ContentType.MESSAGE,
                            rawContent = "प्रिय ग्राहक, आपका बिजली बिल ₹1450 बकाया है। आज रात 9:30 बजे बिजली कट जाएगी। तुरंत अधिकारी 9876543210 पर कॉल करें।",
                            language = "Hindi",
                            riskScore = 88,
                            riskLevel = RiskLevel.HIGH_RISK,
                            category = RiskCategory.BANKING_KYC,
                            signals = listOf(
                                RiskSignal("Power Disconnection Threat", "Coercive urgency regarding immediate electricity disconnection in Hindi."),
                                RiskSignal("Unverified Phone Number", "Directs recipient to call a non-official personal mobile number.")
                            ),
                            recommendedActions = listOf(
                                "Do not call the mobile number provided.",
                                "Pay or check bills only via official electricity board portal or app."
                            )
                        ),
                        ScanResult(
                            contentType = ContentType.URL,
                            rawContent = "http://paytm-cashback-reward-offer-2026.online/claim",
                            language = "English",
                            riskScore = 92,
                            riskLevel = RiskLevel.HIGH_RISK,
                            category = RiskCategory.PHISHING,
                            signals = listOf(
                                RiskSignal("Offline URL Analysis", "This result is based on local URL patterns and threat rules. Live website reputation was not checked."),
                                RiskSignal("Brand Fraud", "Impersonates Paytm brand on unofficial domain."),
                                RiskSignal("High-Risk Extension", "Uses .online generic domain extension.")
                            ),
                            recommendedActions = listOf(
                                "Close the browser tab immediately.",
                                "Never enter UPI PIN or phone numbers on external cashback sites."
                            )
                        ),
                        ScanResult(
                            contentType = ContentType.MESSAGE,
                            rawContent = "Congratulations! You have won Rs 25,00,00,000 in KBC Lottery. Send 2500 processing fees via Google Pay to claim check.",
                            language = "Hinglish",
                            riskScore = 94,
                            riskLevel = RiskLevel.HIGH_RISK,
                            category = RiskCategory.LOTTERY_PRIZE,
                            signals = listOf(
                                RiskSignal("Lottery Scam Pattern", "Requires upfront processing fee for fake winnings."),
                                RiskSignal("Advance Fee Fraud", "Directs recipient to send money via UPI.")
                            ),
                            recommendedActions = listOf(
                                "Ignore and delete the message immediately.",
                                "No legitimate prize requires advance payment or UPI fees."
                            )
                        ),
                        ScanResult(
                            contentType = ContentType.MESSAGE,
                            rawContent = "Your OTP for accessing HDFC NetBanking is 482910. Valid for 10 minutes. Do not share with anyone.",
                            language = "English",
                            riskScore = 8,
                            riskLevel = RiskLevel.SAFE,
                            category = RiskCategory.SAFE_MSG,
                            signals = listOf(
                                RiskSignal("No Suspicious Pattern Found", "Standard bank OTP notification without external links or money requests.")
                            ),
                            recommendedActions = listOf(
                                "Message appears safe. Keep your OTP private."
                            )
                        )
                    )
                    initialRecords.forEach { dao.insertScan(it.toScanEntity()) }
                }

                dao.observeAllScans().collect { entities ->
                    _history.value = entities.map { it.toScanResult() }
                }
            }
        }
    }

    val presetSamples = listOf(
        PresetSample(
            title = "Gujarati KYC",
            language = "Gujarati",
            contentType = ContentType.MESSAGE,
            text = "સાવધાન! તમારું SBI બેંક ખાતું 24 કલાકમાં ફ્રીઝ થઈ જશે. અત્યારે જ KYC અપડેટ કરો: http://sbi-verify.kcy-update.xyz"
        ),
        PresetSample(
            title = "Hindi UPI",
            language = "Hindi",
            contentType = ContentType.MESSAGE,
            text = "आपका पेटीएम कैशबैक ₹5000 स्वीकृत हो गया है। राशि प्राप्त करने के लिए तुरंत यूपीआई पिन दर्ज करें: http://bit.ly/paytm-claim-5000"
        ),
        PresetSample(
            title = "Electricity Scam",
            language = "Hindi",
            contentType = ContentType.MESSAGE,
            text = "प्रिय ग्राहक, आपका बिजली बिल ₹1450 बकाया है। आज रात 9:30 बजे बिजली कट जाएगी। तुरंत अधिकारी 9876543210 पर कॉल करें。"
        ),
        PresetSample(
            title = "Prize Scam",
            language = "Hinglish",
            contentType = ContentType.MESSAGE,
            text = "Congratulations! You have won Rs 25,00,00,000 in KBC Lottery. Send 2500 processing fees via Google Pay to claim check."
        ),
        PresetSample(
            title = "Phishing URL",
            language = "English",
            contentType = ContentType.URL,
            text = "http://secure-icici-netbanking-login-verification-auth.top/login.php"
        )
    )

    suspend fun performScan(type: ContentType, input: String, imageUri: String? = null): ScanResult {
        val result = LocalDetectionEngine.analyzeContent(type, input, imageUri)
        database?.scanDao()?.insertScan(result.toScanEntity())
        return result
    }

    suspend fun saveScan(scanResult: ScanResult) {
        database?.scanDao()?.insertScan(scanResult.toScanEntity())
    }

    suspend fun clearHistory() {
        database?.scanDao()?.deleteAllScans()
    }
}
