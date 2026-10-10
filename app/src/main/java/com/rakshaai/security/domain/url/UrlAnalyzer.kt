package com.rakshaai.security.domain.url

import android.content.Context

object UrlAnalyzer {

    private val compositeProvider = CompositeThreatIntelProvider()

    suspend fun initialize(context: Context) {
        ThreatIntelligenceStore.initialize(context)
    }

    fun preload(context: Context) {
        ThreatIntelligenceStore.preloadAsync(context)
    }

    suspend fun analyzeAsync(urlInput: String, isOnline: Boolean = true): UrlAnalysisResult {
        val parsed = UrlParser.parse(urlInput)
        val features = UrlFeatureExtractor.extract(parsed)
        val threatIntel = compositeProvider.checkUrl(parsed, isOnline)
        return UrlRuleEngine.evaluate(parsed, features, threatIntel)
    }

    fun analyze(urlInput: String): UrlAnalysisResult {
        val parsed = UrlParser.parse(urlInput)
        val features = UrlFeatureExtractor.extract(parsed)
        val threatIntel = ThreatIntelligenceStore.lookup(parsed)
        return UrlRuleEngine.evaluate(parsed, features, threatIntel)
    }
}
