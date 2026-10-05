package com.amoledwatchfaces.solarpath.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.amoledwatchfaces.solarpath.utils.updateComplications
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration

private const val TAG = "SolarRefreshWorker"
private const val WORK_NAME = "solar_periodic_refresh"

@HiltWorker
class SolarRefreshWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Solar periodic refresh worker running - updating complications")
        context.updateComplications()
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val periodicWork = PeriodicWorkRequestBuilder<SolarRefreshWorker>(
                repeatInterval = Duration.ofHours(6)
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWork
            )
            Log.i(TAG, "Scheduled periodic solar refresh work (6 hour interval)")
        }
    }
}
