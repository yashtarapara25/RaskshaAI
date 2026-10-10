package com.rakshaai.security.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey
    val id: String,
    val timestamp: Long,
    val contentType: String,
    val rawContent: String,
    val imageUri: String?,
    val language: String,
    val riskScore: Int,
    val riskLevel: String,
    val category: String,
    val mlClassificationLabel: String,
    val mlScore: Double?,
    val signalsJson: String,
    val highlightedPhrasesJson: String,
    val warningSignsJson: String,
    val recommendedActionsJson: String,
    val preventionGuidance: String,
    val isOfflineProcessed: Boolean,
    val isNotification: Boolean = false
)
