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
import android.graphics.drawable.Icon
import androidx.datastore.core.DataStore
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.amoledwatchfaces.solarpath.R
import com.amoledwatchfaces.solarpath.data.UserPreferences
import com.amoledwatchfaces.solarpath.data.UserPreferencesRepository
import com.amoledwatchfaces.solarpath.presentation.MainActivity
import com.amoledwatchfaces.solarpath.receiver.BlockerBroadcastReceiver
import com.amoledwatchfaces.solarpath.solar.SolarCalculator
import com.amoledwatchfaces.solarpath.solar.SolarData
import com.amoledwatchfaces.solarpath.utils.areLocationPermissionsGranted
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SolarPathComplicationService : SuspendingComplicationDataSourceService() {

    @Inject
    lateinit var dataStore: DataStore<UserPreferences>
    private val preferences by lazy { UserPreferencesRepository(dataStore).getPreferences() }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this, 2001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val sunIcon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.wb_twilight_24px)).build()
        val sampleSolar = SolarData()
        val previewTitle = SolarCalculator.formatAnglesForComplication(sampleSolar, "SUNSET")

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("18:42").build(),
                    contentDescription = PlainComplicationText.Builder("Sunset 18:42").build()
                )
                    .setTitle(PlainComplicationText.Builder(previewTitle).build())
                    .setMonochromaticImage(sunIcon)
                    .setTapAction(null)
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("Sunset 18:42").build(),
                    contentDescription = PlainComplicationText.Builder("Sunset 18:42").build()
                )
                    .setTitle(PlainComplicationText.Builder("Solar Path").build())
                    .setMonochromaticImage(sunIcon)
                    .setTapAction(null)
                    .build()
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                MonochromaticImageComplicationData.Builder(
                    monochromaticImage = sunIcon,
                    contentDescription = PlainComplicationText.Builder("Solar Path").build()
                )
                    .setTapAction(null)
                    .build()
            }
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val prefs = preferences.first()
        val hasPermission = areLocationPermissionsGranted()
        val hasConfiguredLocation = prefs.locationName != "- -" && (prefs.latitude != 0.0 || prefs.longitude != 0.0)
        val isLocationSetup = (hasPermission && (prefs.latitude != 0.0 || prefs.longitude != 0.0 || prefs.locationName != "- -")) || hasConfiguredLocation
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = keyguardManager?.isDeviceLocked == true || keyguardManager?.isKeyguardLocked == true
        val showPrompt = !isLocationSetup && !prefs.isLocationPromptDismissed && !isLocked

        val sunIcon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.wb_twilight_24px)).build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                // SLOT 0: Solar Path angles & next event (or "Open App" tapAction when showing prompt)
                val tapAction = if (showPrompt) openAppIntent() else null

                if (showPrompt) {
                    ShortTextComplicationData.Builder(
                        text = PlainComplicationText.Builder("SETUP").build(),
                        contentDescription = PlainComplicationText.Builder("Heads up! This watch face works best when location is used. Open configuration app to enable location permissions.").build()
                    )
                        .setTitle(PlainComplicationText.Builder("NO_LOCATION").build())
                        .setMonochromaticImage(sunIcon)
                        .setTapAction(tapAction)
                        .build()
                } else {
                    val solar = SolarCalculator.calculateSolarData(prefs.latitude, prefs.longitude)

                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                        .withZone(ZoneId.systemDefault())

                    val eventTimeText = if (solar.nextEventEpoch > 0) {
                        timeFormatter.format(Instant.ofEpochMilli(solar.nextEventEpoch))
                    } else "- -"

                    val titleText = if (solar.nextEventName.isNotEmpty()) {
                        solar.nextEventName.uppercase()
                    } else if (prefs.locationName != "- -") {
                        prefs.locationName
                    } else "SUN"

                    val formattedTitle = SolarCalculator.formatAnglesForComplication(solar, titleText)

                    ShortTextComplicationData.Builder(
                        text = PlainComplicationText.Builder(eventTimeText).build(),
                        contentDescription = PlainComplicationText.Builder("$titleText $eventTimeText").build()
                    )
                        .setTitle(PlainComplicationText.Builder(formattedTitle).build())
                        .setMonochromaticImage(sunIcon)
                        .setTapAction(null)
                        .build()
                }
            }

            ComplicationType.MONOCHROMATIC_IMAGE -> {
                // SLOT 6: "Dismiss" Button
                val tapAction = if (showPrompt) {
                    val intent = Intent(this, DismissPromptActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    PendingIntent.getActivity(
                        this,
                        6001,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                } else {
                    null
                }

                MonochromaticImageComplicationData.Builder(
                    monochromaticImage = sunIcon,
                    contentDescription = PlainComplicationText.Builder("Dismiss").build()
                )
                    .setTapAction(tapAction)
                    .build()
            }

            ComplicationType.LONG_TEXT -> {
                // SLOT 7: Full-Screen Touch Blocker (absorbs touches away from slots 1-5 beneath)
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

                LongTextComplicationData.Builder(
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
