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
import androidx.compose.remote.creation.compose.modifier.clickable
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
import java.time.Instant
import java.time.ZoneId
import androidx.glance.wear.WearWidgetData
import androidx.glance.wear.WearWidgetDocument
import androidx.glance.wear.color
import androidx.glance.wear.core.WearWidgetParams
import androidx.wear.compose.material3.dynamicColorScheme
import androidx.wear.compose.remote.material3.RemoteCard
import androidx.wear.compose.remote.material3.RemoteCardDefaults
import androidx.wear.compose.remote.material3.RemoteColorScheme
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.remote.material3.RemoteText
import com.amoledwatchfaces.solarpath.R
import com.amoledwatchfaces.solarpath.data.UserPreferences
import com.amoledwatchfaces.solarpath.presentation.MainActivity
import com.amoledwatchfaces.solarpath.solar.SolarCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
@AssociateWithGlanceWearWidget(DuskTimesWidget::class)
class DuskTimesWidgetService : GlanceWearWidgetService() {

    @Inject
    lateinit var dataStore: DataStore<UserPreferences>

    override val widget: GlanceWearWidget by lazy {
        DuskTimesWidget(dataStore)
    }
}

class DuskTimesWidget(
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

        val sunsetMinute = solarData?.let { epochToDuskMinute(it.sunsetEpoch) } ?: -1f
        val civilDuskMinute = solarData?.let { epochToDuskMinute(it.civilDuskEpoch) } ?: -1f
        val nauticalDuskMinute = solarData?.let { epochToDuskMinute(it.nauticalDuskEpoch) } ?: -1f
        val astroDuskMinute = solarData?.let { epochToDuskMinute(it.astroDuskEpoch) } ?: -1f
        val midnightMinute = solarData?.let { epochToDuskMinute(it.solarNadirEpoch) } ?: -1f

        return WearWidgetDocument(
            background = WearWidgetBrush.color(backgroundColor)
        ) {
            DuskTimesWidgetContent(
                hasLocation = hasLocation,
                locationName = preferences.locationName,
                sunsetTime = solarData?.let { timeFormat.format(Date(it.sunsetEpoch)) } ?: "- -",
                civilDuskTime = solarData?.let { timeFormat.format(Date(it.civilDuskEpoch)) } ?: "- -",
                nauticalDuskTime = solarData?.let { timeFormat.format(Date(it.nauticalDuskEpoch)) } ?: "- -",
                astroDuskTime = solarData?.let { timeFormat.format(Date(it.astroDuskEpoch)) } ?: "- -",
                midnightTime = solarData?.let { timeFormat.format(Date(it.solarNadirEpoch)) } ?: "- -",
                sunsetTitle = context.getString(R.string.sunset),
                civilTitle = context.getString(R.string.civil_dusk),
                nauticalTitle = context.getString(R.string.nautical_dusk),
                astroTitle = context.getString(R.string.astro_dusk),
                midnightTitle = context.getString(R.string.solar_midnight),
                sunsetMinute = sunsetMinute,
                civilDuskMinute = civilDuskMinute,
                nauticalDuskMinute = nauticalDuskMinute,
                astroDuskMinute = astroDuskMinute,
                midnightMinute = midnightMinute
            )
        }
    }
}

private fun epochToDuskMinute(epoch: Long, zoneId: ZoneId = ZoneId.systemDefault()): Float {
    if (epoch <= 0L) return -1f
    val zdt = Instant.ofEpochMilli(epoch).atZone(zoneId)
    val hour = zdt.hour
    val minute = zdt.minute
    val adjustedHour = if (hour < 12) hour + 24 else hour
    return (adjustedHour * 60 + minute).toFloat()
}

@SuppressLint("RestrictedApi")
@RemoteComposable
@Composable
fun DuskTimesWidgetContent(
    hasLocation: Boolean,
    locationName: String,
    sunsetTime: String,
    civilDuskTime: String,
    nauticalDuskTime: String,
    astroDuskTime: String,
    midnightTime: String,
    sunsetTitle: String,
    civilTitle: String,
    nauticalTitle: String,
    astroTitle: String,
    midnightTitle: String,
    sunsetMinute: Float,
    civilDuskMinute: Float,
    nauticalDuskMinute: Float,
    astroDuskMinute: Float,
    midnightMinute: Float
) {
    val time = RemoteTime()
    val hour = time.Hour()
    val minute = time.Minutes()
    val currentMinute = hour.times(60f).plus(minute)
    val isAfterMidnight = hour.isLessThan(12f.rf)
    val currentAdjustedMinute = currentMinute.plus(isAfterMidnight.select(1440f.rf, 0f.rf))

    val isSunsetPast = if (hasLocation && sunsetMinute >= 0f) currentAdjustedMinute.isGreaterThanOrEqualTo(sunsetMinute.rf) else false.rb
    val isSunsetNext = if (hasLocation && sunsetMinute >= 0f) currentAdjustedMinute.isLessThan(sunsetMinute.rf) else false.rb

    val isCivilPast = if (hasLocation && civilDuskMinute >= 0f) currentAdjustedMinute.isGreaterThanOrEqualTo(civilDuskMinute.rf) else false.rb
    val isCivilNext = if (hasLocation && civilDuskMinute >= 0f && sunsetMinute >= 0f) {
        currentAdjustedMinute.isGreaterThanOrEqualTo(sunsetMinute.rf).and(currentAdjustedMinute.isLessThan(civilDuskMinute.rf))
    } else false.rb

    val isNauticalPast = if (hasLocation && nauticalDuskMinute >= 0f) currentAdjustedMinute.isGreaterThanOrEqualTo(nauticalDuskMinute.rf) else false.rb
    val isNauticalNext = if (hasLocation && nauticalDuskMinute >= 0f && civilDuskMinute >= 0f) {
        currentAdjustedMinute.isGreaterThanOrEqualTo(civilDuskMinute.rf).and(currentAdjustedMinute.isLessThan(nauticalDuskMinute.rf))
    } else false.rb

    val isAstroPast = if (hasLocation && astroDuskMinute >= 0f) currentAdjustedMinute.isGreaterThanOrEqualTo(astroDuskMinute.rf) else false.rb
    val isAstroNext = if (hasLocation && astroDuskMinute >= 0f && nauticalDuskMinute >= 0f) {
        currentAdjustedMinute.isGreaterThanOrEqualTo(nauticalDuskMinute.rf).and(currentAdjustedMinute.isLessThan(astroDuskMinute.rf))
    } else false.rb

    val isMidnightPast = if (hasLocation && midnightMinute >= 0f) currentAdjustedMinute.isGreaterThanOrEqualTo(midnightMinute.rf) else false.rb
    val isMidnightNext = if (hasLocation && midnightMinute >= 0f && astroDuskMinute >= 0f) {
        currentAdjustedMinute.isGreaterThanOrEqualTo(astroDuskMinute.rf).and(currentAdjustedMinute.isLessThan(midnightMinute.rf))
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
                    name = sunsetTitle,
                    time = sunsetTime,
                    isNext = isSunsetNext,
                    isPast = isSunsetPast,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
                MilestoneItemRow(
                    name = civilTitle,
                    time = civilDuskTime,
                    isNext = isCivilNext,
                    isPast = isCivilPast,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
                MilestoneItemRow(
                    name = nauticalTitle,
                    time = nauticalDuskTime,
                    isNext = isNauticalNext,
                    isPast = isNauticalPast,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
                MilestoneItemRow(
                    name = astroTitle,
                    time = astroDuskTime,
                    isNext = isAstroNext,
                    isPast = isAstroPast,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
                MilestoneItemRow(
                    name = midnightTitle,
                    time = midnightTime,
                    isNext = isMidnightNext,
                    isPast = isMidnightPast,
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

@SuppressLint("RestrictedApi")
@RemoteComposable
@Composable
fun MilestoneItemRow(
    name: String,
    time: String,
    isNext: RemoteBoolean,
    isPast: RemoteBoolean,
    colorScheme: RemoteColorScheme
) {
    val nameColor = isNext.select(
        colorScheme.tertiary,
        isPast.select(colorScheme.onSurfaceVariant, colorScheme.onSurface)
    )
    val timeColor = isNext.select(
        colorScheme.tertiaryDim,
        isPast.select(colorScheme.onSurfaceVariant, colorScheme.primaryDim)
    )
    val dotColor = isNext.select(
        colorScheme.primary,
        isPast.select(colorScheme.outlineVariant, colorScheme.outline)
    )
    val dotText = isNext.select("● ".rs, "○ ".rs)

    RemoteRow(
        modifier = RemoteModifier.fillMaxWidth(),
        horizontalArrangement = RemoteArrangement.SpaceBetween,
        verticalAlignment = RemoteAlignment.CenterVertically
    ) {
        RemoteRow(
            modifier = RemoteModifier,
            verticalAlignment = RemoteAlignment.CenterVertically,
            horizontalArrangement = RemoteArrangement.Start
        ) {
            RemoteText(
                text = dotText,
                style = RemoteMaterialTheme.typography.bodySmall,
                color = dotColor
            )
            RemoteText(
                text = name.rs,
                style = RemoteMaterialTheme.typography.bodySmall,
                color = nameColor,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
        RemoteText(
            textAlign = TextAlign.End,
            modifier = RemoteModifier,
            text = time.rs,
            style = RemoteMaterialTheme.typography.bodySmall,
            color = timeColor,
            maxLines = 1
        )
    }
}

@SuppressLint("RestrictedApi")
@RemoteComposable
@Composable
fun MilestoneItemRow(
    name: String,
    time: String,
    isNext: Boolean,
    isPast: Boolean,
    colorScheme: RemoteColorScheme
) {
    MilestoneItemRow(
        name = name,
        time = time,
        isNext = isNext.rb,
        isPast = isPast.rb,
        colorScheme = colorScheme
    )
}
