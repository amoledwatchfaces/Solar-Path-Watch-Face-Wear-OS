package com.amoledwatchfaces.solarpath.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.amoledwatchfaces.solarpath.data.UserPreferences
import com.amoledwatchfaces.solarpath.data.UserPreferencesRepository
import com.amoledwatchfaces.solarpath.utils.updateComplications
import com.amoledwatchfaces.solarpath.workers.LocationWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SystemBroadcastReceiver"

@AndroidEntryPoint
class SystemBroadcastReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Inject
    lateinit var dataStore: DataStore<UserPreferences>

    private val preferences by lazy { UserPreferencesRepository(dataStore).getPreferences() }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == LocationWorker.ACTION_PASSIVE_LOCATION) {
            Log.i(TAG, "Passive location update received")
            scope.launch {
                if (preferences.first().backgroundLocationState) {
                    val request = OneTimeWorkRequestBuilder<LocationWorker>().build()
                    WorkManager.getInstance(context).enqueue(request)
                }
            }
            return
        }

        if (intent.action in listOf(
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_BOOT_COMPLETED,
                "android.intent.action.QUICKBOOT_POWERON",
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_DATE_CHANGED
            )) {
            Log.i(TAG, "System event received: ${intent.action}")
            scope.launch {
                val prefs = preferences.first()
                if (prefs.backgroundLocationState) {
                    LocationWorker.scheduleBackgroundLocation(context, prefs.backgroundLocationRepeatInterval)
                }
                context.updateComplications()
            }
        }
    }
}
