package com.amoledwatchfaces.solarpath.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.datastore.core.DataStore
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.amoledwatchfaces.solarpath.presentation.MainActivity
import com.amoledwatchfaces.solarpath.R
import com.amoledwatchfaces.solarpath.data.UserPreferences
import com.amoledwatchfaces.solarpath.data.UserPreferencesRepository
import com.amoledwatchfaces.solarpath.solar.SolarCalculator
import com.amoledwatchfaces.solarpath.solar.SolarData
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
        val sunIcon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_sun)).build()
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
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val prefs = preferences.first()
        val solar = SolarCalculator.calculateSolarData(prefs.latitude, prefs.longitude)
        val sunIcon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_sun)).build()
        val tapAction = openAppIntent()

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

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(eventTimeText).build(),
                    contentDescription = PlainComplicationText.Builder("$titleText $eventTimeText").build()
                )
                    .setTitle(PlainComplicationText.Builder(formattedTitle).build())
                    .setMonochromaticImage(sunIcon)
                    .setTapAction(tapAction)
                    .build()
            }
            else -> null
        }
    }
}
