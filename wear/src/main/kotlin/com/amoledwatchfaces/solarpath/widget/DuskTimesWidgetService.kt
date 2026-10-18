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
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.text.RemoteTimeDefaults
import androidx.compose.runtime.Composable
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
            locationName = "London"
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

        data class Milestone(val name: String, val epoch: Long)

        val milestones = if (solarData != null) {
            listOf(
                Milestone(context.getString(R.string.sunset), solarData.sunsetEpoch),
                Milestone(context.getString(R.string.civil_dusk), solarData.civilDuskEpoch),
                Milestone(context.getString(R.string.nautical_dusk), solarData.nauticalDuskEpoch),
                Milestone(context.getString(R.string.astro_dusk), solarData.astroDuskEpoch),
                Milestone(context.getString(R.string.solar_midnight), solarData.solarNadirEpoch)
            )
        } else emptyList()

        val nextMilestone = milestones.firstOrNull { it.epoch > currentTime }

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
                nextEpoch = nextMilestone?.epoch ?: 0L,
                sunsetEpoch = solarData?.sunsetEpoch ?: 0L,
                civilEpoch = solarData?.civilDuskEpoch ?: 0L,
                nauticalEpoch = solarData?.nauticalDuskEpoch ?: 0L,
                astroEpoch = solarData?.astroDuskEpoch ?: 0L,
                midnightEpoch = solarData?.solarNadirEpoch ?: 0L,
                currentTime = currentTime
            )
        }
    }
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
    nextEpoch: Long,
    sunsetEpoch: Long,
    civilEpoch: Long,
    nauticalEpoch: Long,
    astroEpoch: Long,
    midnightEpoch: Long,
    currentTime: Long
) {
    RemoteBox(
        modifier = RemoteModifier
            .background(RemoteCardDefaults.cardColors().containerColor)
            .padding(4f.rf)
            .fillMaxSize()
            .clickable(
                pendingIntentAction { context ->
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
            )
    ){
        RemoteColumn(
            modifier = RemoteModifier.fillMaxWidth().fillMaxHeight(),
            verticalArrangement = RemoteArrangement.Top,
            horizontalAlignment = RemoteAlignment.CenterHorizontally
        ) {
            RemoteRow(
                modifier = RemoteModifier.fillMaxWidth().padding(bottom = 2.rdp),
                verticalAlignment = RemoteAlignment.CenterVertically,
                horizontalArrangement = RemoteArrangement.SpaceBetween
            ) {
                RemoteRow(
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
                    isNext = nextEpoch > 0L && sunsetEpoch == nextEpoch,
                    isPast = sunsetEpoch in 1..<currentTime,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
                MilestoneItemRow(
                    name = civilTitle,
                    time = civilDuskTime,
                    isNext = nextEpoch > 0L && civilEpoch == nextEpoch,
                    isPast = civilEpoch in 1..<currentTime,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
                MilestoneItemRow(
                    name = nauticalTitle,
                    time = nauticalDuskTime,
                    isNext = nextEpoch > 0L && nauticalEpoch == nextEpoch,
                    isPast = nauticalEpoch in 1..<currentTime,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
                MilestoneItemRow(
                    name = astroTitle,
                    time = astroDuskTime,
                    isNext = nextEpoch > 0L && astroEpoch == nextEpoch,
                    isPast = astroEpoch in 1..<currentTime,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
                MilestoneItemRow(
                    name = midnightTitle,
                    time = midnightTime,
                    isNext = nextEpoch > 0L && midnightEpoch == nextEpoch,
                    isPast = midnightEpoch in 1..<currentTime,
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
    isNext: Boolean,
    isPast: Boolean,
    colorScheme: RemoteColorScheme
) {
    val nameColor = when {
        isNext -> colorScheme.tertiary
        isPast -> colorScheme.onSurfaceVariant
        else -> colorScheme.onSurface
    }
    val timeColor = when {
        isNext -> colorScheme.tertiaryDim
        isPast -> colorScheme.onSurfaceVariant
        else -> colorScheme.primaryDim
    }

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
            val dotColor = if (isNext) colorScheme.primary else if (isPast) colorScheme.outlineVariant else colorScheme.outline
            RemoteText(
                text = (if (isNext) "● " else "○ ").rs,
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
            modifier = RemoteModifier,
            text = time.rs,
            style = RemoteMaterialTheme.typography.bodySmall,
            color = timeColor,
            maxLines = 1
        )
    }
}
