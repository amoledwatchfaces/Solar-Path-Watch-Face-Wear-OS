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
                Milestone(context.getString(R.string.astro_dawn), solarData.astroDawnEpoch),
                Milestone(context.getString(R.string.nautical_dawn), solarData.nauticalDawnEpoch),
                Milestone(context.getString(R.string.civil_dawn), solarData.civilDawnEpoch),
                Milestone(context.getString(R.string.sunrise), solarData.sunriseEpoch),
                Milestone(context.getString(R.string.solar_noon), solarData.solarNoonEpoch)
            )
        } else emptyList()

        val nextMilestone = milestones.firstOrNull { it.epoch > currentTime }

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
                nextEpoch = nextMilestone?.epoch ?: 0L,
                astroEpoch = solarData?.astroDawnEpoch ?: 0L,
                nauticalEpoch = solarData?.nauticalDawnEpoch ?: 0L,
                civilEpoch = solarData?.civilDawnEpoch ?: 0L,
                sunriseEpoch = solarData?.sunriseEpoch ?: 0L,
                noonEpoch = solarData?.solarNoonEpoch ?: 0L,
                currentTime = currentTime
            )
        }
    }
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
    nextEpoch: Long,
    sunriseEpoch: Long,
    civilEpoch: Long,
    nauticalEpoch: Long,
    astroEpoch: Long,
    noonEpoch: Long,
    currentTime: Long
) {
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
                        contentDescription = "Dawn Icon".rs,
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
                    name = noonTitle,
                    time = noonTime,
                    isNext = nextEpoch > 0L && noonEpoch == nextEpoch,
                    isPast = noonEpoch in 1..<currentTime,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
                MilestoneItemRow(
                    name = sunriseTitle,
                    time = sunriseTime,
                    isNext = nextEpoch > 0L && sunriseEpoch == nextEpoch,
                    isPast = sunriseEpoch in 1..<currentTime,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
                MilestoneItemRow(
                    name = civilTitle,
                    time = civilDawnTime,
                    isNext = nextEpoch > 0L && civilEpoch == nextEpoch,
                    isPast = civilEpoch in 1..<currentTime,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
                MilestoneItemRow(
                    name = nauticalTitle,
                    time = nauticalDawnTime,
                    isNext = nextEpoch > 0L && nauticalEpoch == nextEpoch,
                    isPast = nauticalEpoch in 1..<currentTime,
                    colorScheme = RemoteMaterialTheme.colorScheme
                )
                MilestoneItemRow(
                    name = astroTitle,
                    time = astroDawnTime,
                    isNext = nextEpoch > 0L && astroEpoch == nextEpoch,
                    isPast = astroEpoch in 1..<currentTime,
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
