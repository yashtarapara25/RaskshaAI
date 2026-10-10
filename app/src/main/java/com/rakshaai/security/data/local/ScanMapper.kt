package com.rakshaai.security.data.local

import com.rakshaai.security.data.model.ContentType
import com.rakshaai.security.data.model.RiskCategory
import com.rakshaai.security.data.model.RiskLevel
import com.rakshaai.security.data.model.ScanResult

fun ScanEntity.toScanResult(): ScanResult {
    val ct = try { ContentType.valueOf(contentType) } catch (_: Exception) { ContentType.MESSAGE }
    val rl = try { RiskLevel.valueOf(riskLevel) } catch (_: Exception) { RiskLevel.SAFE }
    val rc = try { RiskCategory.valueOf(category) } catch (_: Exception) { RiskCategory.SAFE_MSG }

    val converters = Converters()
    return ScanResult(
        id = id,
        timestamp = timestamp,
        contentType = ct,
        rawContent = rawContent,
        imageUri = imageUri,
        language = language,
        riskScore = riskScore,
        riskLevel = rl,
        category = rc,
        mlClassificationLabel = mlClassificationLabel,
        mlScore = mlScore,
        signals = converters.toRiskSignals(signalsJson),
        highlightedPhrases = converters.toStringList(highlightedPhrasesJson),
        warningSigns = converters.toStringList(warningSignsJson),
        recommendedActions = converters.toStringList(recommendedActionsJson),
        preventionGuidance = preventionGuidance,
        isOfflineProcessed = isOfflineProcessed,
        isNotification = isNotification
    )
}

fun ScanResult.toScanEntity(): ScanEntity {
    val converters = Converters()
    return ScanEntity(
        id = id,
        timestamp = timestamp,
        contentType = contentType.name,
        rawContent = rawContent,
        imageUri = imageUri,
        language = language,
        riskScore = riskScore,
        riskLevel = riskLevel.name,
        category = category.name,
        mlClassificationLabel = mlClassificationLabel,
        mlScore = mlScore,
        signalsJson = converters.fromRiskSignals(signals),
        highlightedPhrasesJson = converters.fromStringList(highlightedPhrases),
        warningSignsJson = converters.fromStringList(warningSigns),
        recommendedActionsJson = converters.fromStringList(recommendedActions),
        preventionGuidance = preventionGuidance,
        isOfflineProcessed = isOfflineProcessed,
        isNotification = isNotification
    )
}
