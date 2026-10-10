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

import android.app.PendingIntent
import android.content.Intent
import android.app.KeyguardManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.amoledwatchfaces.solarpath.data.UserPreferences
import com.amoledwatchfaces.solarpath.data.UserPreferencesRepository
import com.amoledwatchfaces.solarpath.utils.areLocationPermissionsGranted
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class DismissPromptComplicationService : SuspendingComplicationDataSourceService() {

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

        if (!showPrompt) {
            return when (request.complicationType) {
                ComplicationType.SHORT_TEXT -> {
                    ShortTextComplicationData.Builder(
                        text = PlainComplicationText.Builder("").build(),
                        contentDescription = PlainComplicationText.Builder("").build()
                    )
                        .setTapAction(null)
                        .build()
                }
                else -> null
            }
        }

        val intent = Intent(this, DismissPromptActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapAction = PendingIntent.getActivity(
            this,
            2002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("DISMISS").build(),
                    contentDescription = PlainComplicationText.Builder("Dismiss location setup prompt").build()
                )
                    .setTapAction(tapAction)
                    .build()
            }
            else -> null
        }
    }
}
