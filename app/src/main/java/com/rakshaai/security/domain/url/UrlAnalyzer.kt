package com.rakshaai.security.domain.url

import android.content.Context

object UrlAnalyzer {

    suspend fun initialize(context: Context) {
        ThreatIntelligenceStore.initialize(context)
    }

    fun preload(context: Context) {
        ThreatIntelligenceStore.preloadAsync(context)
    }

    fun analyze(urlInput: String): UrlAnalysisResult {
        val parsed = UrlParser.parse(urlInput)
        val features = UrlFeatureExtractor.extract(parsed)
        val threatIntel = ThreatIntelligenceStore.lookup(parsed)
        return UrlRuleEngine.evaluate(parsed, features, threatIntel)
    }
}
