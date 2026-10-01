package com.amoledwatchfaces.solarpath.workers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.amoledwatchfaces.solarpath.data.UserPreferences
import com.amoledwatchfaces.solarpath.data.UserPreferencesRepository
import com.amoledwatchfaces.solarpath.location.AddressProvider
import com.amoledwatchfaces.solarpath.receiver.SystemBroadcastReceiver
import com.amoledwatchfaces.solarpath.utils.formatCoordinate
import com.amoledwatchfaces.solarpath.utils.updateComplications
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "LocationWorker"

@HiltWorker
class LocationWorker @AssistedInject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val dataStore: DataStore<UserPreferences>,
    private val addressProvider: AddressProvider,
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val preferences by lazy { UserPreferencesRepository(dataStore).getPreferences() }

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        Log.i(TAG, "Worker running")
        return try {
            val location = getCurrentLocation() ?: getLastLocation()
            if (location == null) {
                Log.i(TAG, "Failed to obtain Location - retry later")
                Result.retry()
            } else {
                Log.i(TAG, "Location Received: ${location.latitude}, ${location.longitude}")
                val repository = preferences.first()

                val results = FloatArray(1)
                Location.distanceBetween(repository.latitude, repository.longitude, location.latitude, location.longitude, results)
                val distance = results[0]

                if (distance > 2000 || repository.latitude == 0.0) {
                    Log.i(TAG, "Significant location change detected: ${distance}m")
                    val addressInfo = addressProvider.getAddressFromLocation(location.latitude, location.longitude)
                    val cityName = addressInfo?.city ?: "Unknown"
                    val subName = addressInfo?.subName ?: "${formatCoordinate(location.latitude, true)} ${formatCoordinate(location.longitude, false)}"

                    dataStore.updateData {
                        it.copy(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            locationName = cityName,
                            locationSubName = subName
                        )
                    }

                    applicationContext.updateComplications()
                } else {
                    Log.i(TAG, "Location change too small ($distance m). Skipping update.")
                }
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in LocationWorker: ${e.message}", e)
            Result.failure()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            val currentLocationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setGranularity(Granularity.GRANULARITY_COARSE)
                .setDurationMillis(10000)
                .build()

            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(currentLocationRequest, cts.token)
                .addOnSuccessListener { location -> continuation.resume(location) }
                .addOnFailureListener { exception -> continuation.resumeWithException(exception) }

            continuation.invokeOnCancellation { cts.cancel() }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location -> continuation.resume(location) }
                .addOnFailureListener { continuation.resume(null) }
        }
    }

    companion object {
        const val ACTION_PASSIVE_LOCATION = "com.amoledwatchfaces.solarpath.ACTION_PASSIVE_LOCATION"

        @SuppressLint("MissingPermission")
        fun registerPassiveLocationUpdates(context: Context) {
            Log.i(TAG, "Registering passive location updates")
            val intent = Intent(context, SystemBroadcastReceiver::class.java)
            intent.action = ACTION_PASSIVE_LOCATION
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            val request = LocationRequest.Builder(Priority.PRIORITY_PASSIVE, 600000) // 10 minutes
                .setGranularity(Granularity.GRANULARITY_COARSE)
                .setMinUpdateDistanceMeters(500f)
                .build()

            LocationServices.getFusedLocationProviderClient(context).requestLocationUpdates(request, pendingIntent)
        }

        fun unregisterPassiveLocationUpdates(context: Context) {
            Log.i(TAG, "Unregistering passive location updates")
            val intent = Intent(context, SystemBroadcastReceiver::class.java)
            intent.action = ACTION_PASSIVE_LOCATION
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            LocationServices.getFusedLocationProviderClient(context).removeLocationUpdates(pendingIntent)
        }

        fun scheduleBackgroundLocation(context: Context, repeatInterval: Long) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .setRequiresBatteryNotLow(true)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<LocationWorker>(
                repeatInterval = Duration.ofMinutes(repeatInterval.coerceAtLeast(15))
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            Log.i(TAG, "Enqueueing Periodic Work")

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "background_location",
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest
            )
            registerPassiveLocationUpdates(context)
        }

        fun enqueueLocationUpdate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<LocationWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "one_time_location",
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest
            )
        }
    }
}
