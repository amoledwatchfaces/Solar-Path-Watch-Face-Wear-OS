package com.amoledwatchfaces.solarpath

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.datastore.core.DataStore
import com.amoledwatchfaces.solarpath.data.UserPreferences
import com.amoledwatchfaces.solarpath.data.UserPreferencesRepository
import com.amoledwatchfaces.solarpath.utils.setLauncherVisibility
import com.amoledwatchfaces.solarpath.utils.updateComplications
import com.amoledwatchfaces.solarpath.workers.SolarRefreshWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var dataStore: DataStore<UserPreferences>

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        SolarRefreshWorker.schedule(this)
        updateComplications()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = UserPreferencesRepository(dataStore).getPreferences().first()
                if (prefs.hideAppFromLauncher) {
                    setLauncherVisibility(true)
                }
            } catch (_: Exception) { }
        }
    }
}
