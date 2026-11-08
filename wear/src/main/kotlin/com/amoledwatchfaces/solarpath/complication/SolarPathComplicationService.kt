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
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.text.format.DateFormat
import androidx.datastore.core.DataStore
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.CountDownTimeReference
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.amoledwatchfaces.solarpath.R
import com.amoledwatchfaces.solarpath.data.UserPreferences
import com.amoledwatchfaces.solarpath.data.UserPreferencesRepository
import com.amoledwatchfaces.solarpath.presentation.MainActivity
import com.amoledwatchfaces.solarpath.solar.SolarCalculator
import com.amoledwatchfaces.solarpath.solar.SolarData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Wear OS complication service providing solar milestone data.
 *
 * - When requested by the watch face's internal dial slot (instance ID 6), it provides
 *   astronomical angle data encoded in [COMPLICATION.TITLE] for rendering 24-hour celestial arcs.
 * - When requested by user-facing complication slots (slots 7, 8, or any third-party watch face),
 *   it provides clean milestone information (time, title, dynamic icon) without raw angle codes.
 */
@AndroidEntryPoint
class SolarPathComplicationService : SuspendingComplicationDataSourceService() {

    companion object {
        const val INTERNAL_DIAL_SLOT_ID = 6
        private const val REQUEST_CODE_OPEN_APP = 2001
    }

    @Inject
    lateinit var dataStore: DataStore<UserPreferences>
    private val preferences by lazy { UserPreferencesRepository(dataStore).getPreferences() }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val sunIcon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_sunset)).build()
        val eventName = "Sunset"
        val timeText = "18:42"

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(eventName).build(),
                    contentDescription = PlainComplicationText.Builder("$eventName $timeText").build()
                )
                    .setTitle(PlainComplicationText.Builder(timeText).build())
                    .setMonochromaticImage(sunIcon)
                    .setTapAction(null)
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(eventName).build(),
                    contentDescription = PlainComplicationText.Builder("$eventName $timeText").build()
                )
                    .setTitle(PlainComplicationText.Builder(timeText).build())
                    .setMonochromaticImage(sunIcon)
                    .setTapAction(null)
                    .build()
            }
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val prefs = preferences.first()

        // Internal watch face dial slot (slotId 6): provide encoded angles without tapAction
        if (request.complicationInstanceId == INTERNAL_DIAL_SLOT_ID) {
            val solar = SolarCalculator.calculateSolarData(prefs.latitude, prefs.longitude, trackingMode = prefs.complicationTrackingMode)
            val sunIcon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.wb_twilight_24px)).build()

            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                .withZone(ZoneId.systemDefault())

            val eventTimeText = if (solar.nextEventEpoch > 0) {
                timeFormatter.format(Instant.ofEpochMilli(solar.nextEventEpoch))
            } else "- -"

            val titleText = if (solar.nextEventNameResId != 0) {
                getString(solar.nextEventNameResId).uppercase()
            } else if (solar.nextEventName.isNotEmpty()) {
                solar.nextEventName.uppercase()
            } else if (prefs.locationName != "- -") {
                prefs.locationName
            } else "SUN"

            val isLocationAvailable = prefs.latitude != 0.0 || prefs.longitude != 0.0
            val formattedTitle = SolarCalculator.formatAnglesForComplication(solar, titleText, isLocationAvailable)

            return when (request.complicationType) {
                ComplicationType.SHORT_TEXT -> {
                    ShortTextComplicationData.Builder(
                        text = PlainComplicationText.Builder(eventTimeText).build(),
                        contentDescription = PlainComplicationText.Builder("$titleText $eventTimeText").build()
                    )
                        .setTitle(PlainComplicationText.Builder(formattedTitle).build())
                        .setMonochromaticImage(sunIcon)
                        .setTapAction(null)
                        .build()
                }
                else -> null
            }
        }

        // Public complication slots: clean time, title-case milestone name, and dynamic icon
        val solar = SolarCalculator.calculateSolarData(prefs.latitude, prefs.longitude, trackingMode = prefs.complicationTrackingMode)
        val eventTitle = getShortMilestoneTitle(solar)
        val iconRes = getMilestoneIcon(solar.nextEventName)
        val eventIcon = MonochromaticImage.Builder(Icon.createWithResource(this, iconRes)).build()
        val tapAction = openSolarPathAppIntent(REQUEST_CODE_OPEN_APP)

        val timeFormat = DateFormat.getTimeFormat(this)
        val eventTimeText = if (solar.nextEventEpoch > 0) {
            timeFormat.format(Date(solar.nextEventEpoch))
        } else "- -"

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(eventTitle).build(),
                    contentDescription = PlainComplicationText.Builder("$eventTitle $eventTimeText").build()
                )
                    .setTitle(PlainComplicationText.Builder(eventTimeText).build())
                    .setMonochromaticImage(eventIcon)
                    .setTapAction(tapAction)
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(eventTitle).build(),
                    contentDescription = PlainComplicationText.Builder("$eventTitle $eventTimeText").build()
                )
                    .setTitle(PlainComplicationText.Builder(eventTimeText).build())
                    .setMonochromaticImage(eventIcon)
                    .setTapAction(tapAction)
                    .build()
            }
            else -> null
        }
    }
}

/**
 * Public standalone Wear OS complication providing countdown & progress towards the upcoming solar event.
 */
@AndroidEntryPoint
class SolarPathCountdownComplicationService : SuspendingComplicationDataSourceService() {

    companion object {
        private const val REQUEST_CODE_OPEN_APP = 2002
    }

    @Inject
    lateinit var dataStore: DataStore<UserPreferences>
    private val preferences by lazy { UserPreferencesRepository(dataStore).getPreferences() }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val sunIcon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_sunset)).build()
        val sampleCountdown = "6h 45m"

        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = 40f,
                    min = 0f,
                    max = 100f,
                    contentDescription = PlainComplicationText.Builder(getString(R.string.solar_path_countdown_comp_name)).build()
                )
                    .setText(PlainComplicationText.Builder(sampleCountdown).build())
                    .setMonochromaticImage(sunIcon)
                    .setTapAction(null)
                    .build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(sampleCountdown).build(),
                    contentDescription = PlainComplicationText.Builder(getString(R.string.solar_path_countdown_comp_name)).build()
                )
                    .setMonochromaticImage(sunIcon)
                    .setTapAction(null)
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("Sunset in: $sampleCountdown").build(),
                    contentDescription = PlainComplicationText.Builder(getString(R.string.solar_path_countdown_comp_name)).build()
                )
                    .setMonochromaticImage(sunIcon)
                    .setTapAction(null)
                    .build()
            }
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val prefs = preferences.first()
        val solar = SolarCalculator.calculateSolarData(prefs.latitude, prefs.longitude, trackingMode = prefs.complicationTrackingMode)
        val eventTitle = getShortMilestoneTitle(solar)
        val iconRes = getMilestoneIcon(solar.nextEventName)
        val eventIcon = MonochromaticImage.Builder(Icon.createWithResource(this, iconRes)).build()
        val tapAction = openSolarPathAppIntent(REQUEST_CODE_OPEN_APP)

        val nextEpoch = solar.nextEventEpoch
        val prevEpoch = solar.prevEventEpoch
        val currentMillis = System.currentTimeMillis()

        // Total minutes for the active milestone interval
        val totalRangeMinutes = if (nextEpoch > prevEpoch && prevEpoch > 0) {
            ((nextEpoch - prevEpoch) / 60000f).coerceAtLeast(1f)
        } else 720f

        // Remaining minutes until next milestone (matching SunriseSunsetRV countdown draining behavior)
        val remainingRange = if (nextEpoch > currentMillis) {
            ((nextEpoch - currentMillis) / 60000f).coerceIn(0f, totalRangeMinutes)
        } else 0f

        val targetInstant = if (nextEpoch > 0) Instant.ofEpochMilli(nextEpoch) else Instant.ofEpochMilli(currentMillis + 3600000L)

        val countdownText = TimeDifferenceComplicationText.Builder(
            TimeDifferenceStyle.SHORT_DUAL_UNIT,
            CountDownTimeReference(targetInstant)
        ).build()

        return when (request.complicationType) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = remainingRange,
                    min = 0f,
                    max = totalRangeMinutes,
                    contentDescription = PlainComplicationText.Builder(getString(R.string.solar_path_countdown_comp_name)).build()
                )
                    .setText(countdownText)
                    .setMonochromaticImage(eventIcon)
                    .setTapAction(tapAction)
                    .build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = countdownText,
                    contentDescription = TimeDifferenceComplicationText.Builder(
                        TimeDifferenceStyle.SHORT_DUAL_UNIT,
                        CountDownTimeReference(targetInstant)
                    ).setText("$eventTitle in: ^1").build()
                )
                    .setMonochromaticImage(eventIcon)
                    .setTapAction(tapAction)
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                val longCountdown = TimeDifferenceComplicationText.Builder(
                    TimeDifferenceStyle.SHORT_DUAL_UNIT,
                    CountDownTimeReference(targetInstant)
                ).setText("$eventTitle in: ^1").build()

                LongTextComplicationData.Builder(
                    text = longCountdown,
                    contentDescription = longCountdown
                )
                    .setMonochromaticImage(eventIcon)
                    .setTapAction(tapAction)
                    .build()
            }
            else -> null
        }
    }
}

/**
 * Shared helper functions for complication data sources.
 */
private fun Context.openSolarPathAppIntent(requestCode: Int): PendingIntent {
    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
        this, requestCode, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun getMilestoneIcon(eventName: String): Int {
    return when {
        eventName.contains("Sunrise", ignoreCase = true) -> R.drawable.ic_sunrise
        eventName.contains("Sunset", ignoreCase = true) -> R.drawable.ic_sunset
        eventName.contains("Noon", ignoreCase = true) -> R.drawable.ic_sun
        eventName.contains("Dawn", ignoreCase = true) -> R.drawable.ic_dawn
        eventName.contains("Dusk", ignoreCase = true) -> R.drawable.ic_dusk
        eventName.contains("Midnight", ignoreCase = true) || eventName.contains("Nadir", ignoreCase = true) -> R.drawable.wb_twilight_24px
        else -> R.drawable.wb_twilight_24px
    }
}

private fun Context.getShortMilestoneTitle(solar: SolarData): String {
    return when (solar.nextEventNameResId) {
        R.string.solar_noon -> "Noon"
        R.string.solar_midnight -> "Midnight"
        R.string.astro_dawn, R.string.civil_dawn, R.string.nautical_dawn -> "Dawn"
        R.string.astro_dusk, R.string.civil_dusk, R.string.nautical_dusk -> "Dusk"
        R.string.sunrise -> "Sunrise"
        R.string.sunset -> "Sunset"
        else -> if (solar.nextEventNameResId != 0) getString(solar.nextEventNameResId) else if (solar.nextEventName.isNotEmpty()) solar.nextEventName else "Sun"
    }
}
