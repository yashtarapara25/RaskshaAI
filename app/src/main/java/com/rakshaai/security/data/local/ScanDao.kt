package com.rakshaai.security.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity)

    @Query("SELECT * FROM scans ORDER BY timestamp DESC")
    fun observeAllScans(): Flow<List<ScanEntity>>

    @Query("SELECT COUNT(*) FROM scans WHERE rawContent = :content AND timestamp >= :minTimestamp")
    suspend fun countRecentScansWithContent(content: String, minTimestamp: Long): Int

    @Query("DELETE FROM scans")
    suspend fun deleteAllScans()
}
