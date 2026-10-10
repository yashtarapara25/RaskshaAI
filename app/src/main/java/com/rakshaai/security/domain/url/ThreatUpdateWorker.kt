package com.rakshaai.security.domain.url

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rakshaai.security.data.preference.SecurityPreferencesRepository
import java.util.concurrent.TimeUnit

class ThreatUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "rakshaai_threat_intelligence_update"

        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ThreatUpdateWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancelWork(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        SecurityPreferencesRepository.initialize(applicationContext)
        val settings = SecurityPreferencesRepository.settingsState.value

        if (!settings.automaticThreatUpdatesEnabled) {
            return Result.success()
        }

        return try {
            ThreatIntelligenceStore.initialize(applicationContext)
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }
}
