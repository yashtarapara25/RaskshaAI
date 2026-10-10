package com.rakshaai.security.domain.apk

import com.rakshaai.security.data.model.RiskLevel

data class ApkMetadata(
    val fileName: String,
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val fileSize: Long,
    val sha256Hash: String,
    val isFromUnknownSource: Boolean = true
)

data class ApkScanResult(
    val metadata: ApkMetadata,
    val riskLevel: RiskLevel,
    val riskScore: Int,
    val threatStatusText: String,
    val warnings: List<String>,
    val scanTimestamp: Long = System.currentTimeMillis()
)
