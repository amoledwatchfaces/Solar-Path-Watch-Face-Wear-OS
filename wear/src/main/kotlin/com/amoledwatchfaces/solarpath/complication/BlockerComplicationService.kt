/*
 * Copyright (C) 2026 amoledwatchfaces™
 *
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0.html
 */
package com.amoledwatchfaces.solarpath.complication

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.amoledwatchfaces.solarpath.data.UserPreferences
import com.amoledwatchfaces.solarpath.data.UserPreferencesRepository
import com.amoledwatchfaces.solarpath.receiver.BlockerBroadcastReceiver
import com.amoledwatchfaces.solarpath.utils.areLocationPermissionsGranted
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class BlockerComplicationService : SuspendingComplicationDataSourceService() {

    @Inject
    lateinit var dataStore: DataStore<UserPreferences>
    private val preferences by lazy { UserPreferencesRepository(dataStore).getPreferences() }

    override fun getPreviewData(type: ComplicationType): ComplicationData? = null

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val prefs = preferences.first()
        val hasPermission = areLocationPermissionsGranted()
        val hasConfiguredLocation = prefs.locationName != "- -" && (prefs.latitude != 0.0 || prefs.longitude != 0.0)
        val isLocationSetup = (hasPermission && (prefs.latitude != 0.0 || prefs.longitude != 0.0 || prefs.locationName != "- -")) || hasConfiguredLocation
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = keyguardManager?.isDeviceLocked == true || keyguardManager?.isKeyguardLocked == true
        val showPrompt = !isLocationSetup && !prefs.isLocationPromptDismissed && !isLocked

        val tapAction = if (showPrompt) {
            val intent = Intent(this, BlockerBroadcastReceiver::class.java)
            PendingIntent.getBroadcast(
                this,
                7001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
        }

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("").build(),
                    contentDescription = PlainComplicationText.Builder("").build()
                )
                    .setTapAction(tapAction)
                    .build()
            }
            else -> null
        }
    }
}
