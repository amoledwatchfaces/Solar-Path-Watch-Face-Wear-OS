package com.amoledwatchfaces.solarpath.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.format.DateFormat
import androidx.compose.remote.creation.compose.action.pendingIntentAction
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.vectorResource
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteTime
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.text.RemoteTimeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.toColorInt
import androidx.datastore.core.DataStore
import androidx.glance.wear.AssociateWithGlanceWearWidget
import androidx.glance.wear.GlanceWearWidget
import androidx.glance.wear.GlanceWearWidgetService
import androidx.glance.wear.WearWidgetBrush
import androidx.glance.wear.WearWidgetData
import androidx.glance.wear.WearWidgetDocument
import androidx.glance.wear.color
import androidx.glance.wear.core.WearWidgetParams
import androidx.wear.compose.material3.dynamicColorScheme
import androidx.wear.compose.remote.material3.RemoteCard
import androidx.wear.compose.remote.material3.RemoteCardDefaults
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.remote.material3.RemoteText
import com.amoledwatchfaces.solarpath.R
import com.amoledwatchfaces.solarpath.data.UserPreferences
import com.amoledwatchfaces.solarpath.presentation.MainActivity
import com.amoledwatchfaces.solarpath.solar.SolarCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
@AssociateWithGlanceWearWidget(DawnTimesWidget::class)
class DawnTimesWidgetService : GlanceWearWidgetService() {

    @Inject
    lateinit var dataStore: DataStore<UserPreferences>

    override val widget: GlanceWearWidget by lazy {
        DawnTimesWidget(dataStore)
    }
}

class DawnTimesWidget(
    private val dataStore: DataStore<UserPreferences>? = null
) : GlanceWearWidget() {

    override suspend fun provideWidgetData(
        context: Context,
        params: WearWidgetParams
    ): WearWidgetData {
        val preferences = dataStore?.data?.first() ?: UserPreferences(
            latitude = 51.5074,
            longitude = -0.1278,
            locationName = "Bzince pod Javorinou"
        )
        val backgroundColor = dynamicColorScheme(context)?.surfaceContainer?.rc ?: RemoteColor(color = "#332e3c".toColorInt())

        val hasLocation = preferences.latitude != 0.0 || preferences.longitude != 0.0
        val currentTime = System.currentTimeMillis()

        val solarData = if (hasLocation) {
            SolarCalculator.calculateSolarData(
                lat = preferences.latitude,
                lon = preferences.longitude,
                currentTimeMillis = currentTime
            )
        } else null

        val timeFormat = DateFormat.getTimeFormat(context)

        val astroMinute = solarData?.let { epochToMinuteOfDay(it.astroDawnEpoch) } ?: -1f
        val nauticalMinute = solarData?.let { epochToMinuteOfDay(it.nauticalDawnEpoch) } ?: -1f
        val civilMinute = solarData?.let { epochToMinuteOfDay(it.civilDawnEpoch) } ?: -1f
        val sunriseMinute = solarData?.let { epochToMinuteOfDay(it.sunriseEpoch) } ?: -1f
        val noonMinute = solarData?.let { epochToMinuteOfDay(it.solarNoonEpoch) } ?: -1f

        return WearWidgetDocument(
            background = WearWidgetBrush.color(backgroundColor)
        ) {
            DawnTimesWidgetContent(
                hasLocation = hasLocation,
                locationName = preferences.locationName,
                astroDawnTime = solarData?.let { timeFormat.format(Date(it.astroDawnEpoch)) } ?: "- -",
                nauticalDawnTime = solarData?.let { timeFormat.format(Date(it.nauticalDawnEpoch)) } ?: "- -",
                civilDawnTime = solarData?.let { timeFormat.format(Date(it.civilDawnEpoch)) } ?: "- -",
                sunriseTime = solarData?.let { timeFormat.format(Date(it.sunriseEpoch)) } ?: "- -",
                noonTime = solarData?.let { timeFormat.format(Date(it.solarNoonEpoch)) } ?: "- -",
                astroTitle = context.getString(R.string.astro_dawn),
                nauticalTitle = context.getString(R.string.nautical_dawn),
                civilTitle = context.getString(R.string.civil_dawn),
                sunriseTitle = context.getString(R.string.sunrise),
                noonTitle = context.getString(R.string.solar_noon),
                astroDawnMinute = astroMinute,
                nauticalDawnMinute = nauticalMinute,
                civilDawnMinute = civilMinute,
                sunriseMinute = sunriseMinute,
                noonMinute = noonMinute
            )
        }
    }
}

private fun epochToMinuteOfDay(epoch: Long, zoneId: ZoneId = ZoneId.systemDefault()): Float {
    if (epoch <= 0L) return -1f
    val zdt = Instant.ofEpochMilli(epoch).atZone(zoneId)
    return (zdt.hour * 60 + zdt.minute).toFloat()
}

@SuppressLint("RestrictedApi")
@RemoteComposable
@Composable
fun DawnTimesWidgetContent(
    hasLocation: Boolean,
    locationName: String,
    sunriseTime: String,
    civilDawnTime: String,
    nauticalDawnTime: String,
    astroDawnTime: String,
    noonTime: String,
    sunriseTitle: String,
    civilTitle: String,
    nauticalTitle: String,
    astroTitle: String,
    noonTitle: String,
    sunriseMinute: Float,
    civilDawnMinute: Float,
    nauticalDawnMinute: Float,
    astroDawnMinute: Float,
    noonMinute: Float
) {
    val time = RemoteTime()
    val hour = time.Hour()
    val minute = time.Minutes()
    val currentMinute = hour.times(60f).plus(minute)

    val isAstroPast = if (hasLocation && astroDawnMinute >= 0f) currentMinute.isGreaterThanOrEqualTo(astroDawnMinute.rf) else false.rb
    val isAstroNext = if (hasLocation && astroDawnMinute >= 0f) currentMinute.isLessThan(astroDawnMinute.rf) else false.rb

    val isNauticalPast = if (hasLocation && nauticalDawnMinute >= 0f) currentMinute.isGreaterThanOrEqualTo(nauticalDawnMinute.rf) else false.rb
    val isNauticalNext = if (hasLocation && nauticalDawnMinute >= 0f && astroDawnMinute >= 0f) {
        currentMinute.isGreaterThanOrEqualTo(astroDawnMinute.rf).and(currentMinute.isLessThan(nauticalDawnMinute.rf))
    } else false.rb

    val isCivilPast = if (hasLocation && civilDawnMinute >= 0f) currentMinute.isGreaterThanOrEqualTo(civilDawnMinute.rf) else false.rb
    val isCivilNext = if (hasLocation && civilDawnMinute >= 0f && nauticalDawnMinute >= 0f) {
        currentMinute.isGreaterThanOrEqualTo(nauticalDawnMinute.rf).and(currentMinute.isLessThan(civilDawnMinute.rf))
    } else false.rb

    val isSunrisePast = if (hasLocation && sunriseMinute >= 0f) currentMinute.isGreaterThanOrEqualTo(sunriseMinute.rf) else false.rb
    val isSunriseNext = if (hasLocation && sunriseMinute >= 0f && civilDawnMinute >= 0f) {
        currentMinute.isGreaterThanOrEqualTo(civilDawnMinute.rf).and(currentMinute.isLessThan(sunriseMinute.rf))
    } else false.rb

    val isNoonPast = if (hasLocation && noonMinute >= 0f) currentMinute.isGreaterThanOrEqualTo(noonMinute.rf) else false.rb
    val isNoonNext = if (hasLocation && noonMinute >= 0f && sunriseMinute >= 0f) {
        currentMinute.isGreaterThanOrEqualTo(sunriseMinute.rf).and(currentMinute.isLessThan(noonMinute.rf))
    } else false.rb

    RemoteBox(
        modifier = RemoteModifier
            .background(RemoteCardDefaults.cardColors().containerColor)
            .padding(4f.rf)
            .fillMaxSize()
    ){
        RemoteColumn(
            modifier = RemoteModifier.fillMaxWidth().fillMaxHeight(),
            verticalArrangement = RemoteArrangement.Top,
            horizontalAlignment = RemoteAlignment.CenterHorizontally
        ) {
            RemoteRow(
                modifier = RemoteModifier.padding(bottom = 2.rdp),
                verticalAlignment = RemoteAlignment.CenterVertically,
                horizontalArrangement = RemoteArrangement.SpaceBetween
            ) {
                RemoteRow(
                    modifier = RemoteModifier.weight(5f),
                    verticalAlignment = RemoteAlignment.CenterVertically,
                    horizontalArrangement = RemoteArrangement.spacedBy(2.rdp)
                ){
                    RemoteIcon(
                        modifier = RemoteModifier.size(16.rdp),
                        imageVector =
                            if (!hasLocation) RemoteImageVector.vectorResource(R.drawable.location_disabled_24px)
                            else RemoteImageVector.vectorResource(R.drawable.location_city_24px),
                        contentDescription = "Dusk Icon".rs,
                        tint = RemoteMaterialTheme.colorScheme.secondaryDim
                    )
                    RemoteText(
                        text = if (!hasLocation) RemoteString("- -") else locationName.rs,
                        style = RemoteMaterialTheme.typography.titleSmall,
                        color = RemoteMaterialTheme.colorScheme.secondaryDim,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
                RemoteText(
                    modifier = RemoteModifier.weight(2f),
                    textAlign = TextAlign.End,
                    style = RemoteMaterialTheme.typography.labelMedium,
                    color = RemoteMaterialTheme.colorScheme.secondary,
                    text = RemoteTimeDefaults.defaultTimeString()
                )
            }
            RemoteColumn(
                modifier = RemoteModifier.fillMaxWidth().fillMaxHeight(),
                verticalArrangement = RemoteArrangement.Center,
                horizontalAlignment = RemoteAlignment.CenterHorizontally
            ){
                if (!hasLocation) {
                    RemoteText(
                        text = "Set location in app".rs,
                        style = RemoteMaterialTheme.typography.bodySmall,
                        color = RemoteMaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Milestone Rows
                MilestoneItemRow(
                    name = noonTitle,
                    time = noonTime,
                    isNext = isNoonNext,
                    isPast = isNoonPast,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
                MilestoneItemRow(
                    name = sunriseTitle,
                    time = sunriseTime,
                    isNext = isSunriseNext,
                    isPast = isSunrisePast,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
                MilestoneItemRow(
                    name = civilTitle,
                    time = civilDawnTime,
                    isNext = isCivilNext,
                    isPast = isCivilPast,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
                MilestoneItemRow(
                    name = nauticalTitle,
                    time = nauticalDawnTime,
                    isNext = isNauticalNext,
                    isPast = isNauticalPast,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
                MilestoneItemRow(
                    name = astroTitle,
                    time = astroDawnTime,
                    isNext = isAstroNext,
                    isPast = isAstroPast,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
            }
        }
    }
    // Only Nicer Click effect
    RemoteCard(
        colors = RemoteCardDefaults.cardColors(containerColor = RemoteColor(Color.TRANSPARENT)),
        modifier =
            RemoteModifier
                .fillMaxSize(),
        onClick = pendingIntentAction { context ->
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    ){}
}
