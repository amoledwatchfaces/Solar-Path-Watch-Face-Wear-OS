package com.amoledwatchfaces.solarpath.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.format.DateFormat
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.action.pendingIntentAction
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.vectorResource
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteTime
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.cos
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.text.RemoteTimeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import androidx.wear.compose.remote.material3.RemoteColorScheme
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.remote.material3.RemoteText
import com.amoledwatchfaces.solarpath.R
import com.amoledwatchfaces.solarpath.data.UserPreferences
import com.amoledwatchfaces.solarpath.presentation.MainActivity
import com.amoledwatchfaces.solarpath.presentation.ui.appColorScheme
import com.amoledwatchfaces.solarpath.solar.SolarCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos as mathCos
import kotlin.math.sin as mathSin
import kotlin.math.tan as mathTan

@AndroidEntryPoint
@AssociateWithGlanceWearWidget(SolarGraphWidget::class)
class SolarGraphWidgetService : GlanceWearWidgetService() {

    @Inject
    lateinit var dataStore: DataStore<UserPreferences>

    override val widget: GlanceWearWidget by lazy {
        SolarGraphWidget(dataStore)
    }
}

class SolarGraphWidget(
    private val dataStore: DataStore<UserPreferences>? = null
) : GlanceWearWidget() {

    @SuppressLint("RestrictedApi")
    override suspend fun provideWidgetData(
        context: Context,
        params: WearWidgetParams
    ): WearWidgetData {
        val preferences = dataStore?.data?.first() ?: UserPreferences(
            latitude = 51.5074,
            longitude = -0.1278,
            locationName = "Bzince pod Javorinou"
        )
        val colorScheme = dynamicColorScheme(context) ?: appColorScheme
        val remoteColorScheme = RemoteColorScheme(colorScheme)
        val backgroundColor = colorScheme.surfaceContainer.rc

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

        val sunriseMinute = solarData?.let { epochToMinuteOfDay(it.sunriseEpoch) } ?: -1f
        val sunsetMinute = solarData?.let { epochToMinuteOfDay(it.sunsetEpoch) } ?: -1f
        val noonMinute = solarData?.let { epochToMinuteOfDay(it.solarNoonEpoch) } ?: -1f

        return WearWidgetDocument(
            background = WearWidgetBrush.color(backgroundColor)
        ) {
            RemoteMaterialTheme(colorScheme = remoteColorScheme) {
                SolarGraphWidgetContent(
                    hasLocation = hasLocation,
                    locationName = preferences.locationName,
                    sunriseTime = solarData?.let { if (it.sunriseEpoch > 0L) timeFormat.format(Date(it.sunriseEpoch)) else "- -" } ?: "- -",
                    sunsetTime = solarData?.let { if (it.sunsetEpoch > 0L) timeFormat.format(Date(it.sunsetEpoch)) else "- -" } ?: "- -",
                    sunriseTitle = context.getString(R.string.sunrise),
                    sunsetTitle = context.getString(R.string.sunset),
                    locationNotSetText = context.getString(R.string.widget_location_not_set),
                    sunriseMinute = sunriseMinute,
                    sunsetMinute = sunsetMinute,
                    noonMinute = noonMinute
                )
            }
        }
    }
}

private fun epochToMinuteOfDay(epoch: Long, zoneId: ZoneId = ZoneId.systemDefault()): Float {
    if (epoch <= 0L) return -1f
    val zdt = Instant.ofEpochMilli(epoch).atZone(zoneId)
    return (zdt.hour * 60 + zdt.minute).toFloat()
}

@SuppressLint("RestrictedApi", "WearRecents")
@RemoteComposable
@Composable
fun SolarGraphWidgetContent(
    hasLocation: Boolean,
    locationName: String,
    sunriseTime: String,
    sunsetTime: String,
    sunriseTitle: String,
    sunsetTitle: String,
    locationNotSetText: String,
    sunriseMinute: Float,
    sunsetMinute: Float,
    noonMinute: Float
) {
    RemoteBox(
        modifier = RemoteModifier
            .background(RemoteCardDefaults.cardColors().containerColor)
            .padding(4f.rf)
            .fillMaxSize()
    ) {
        RemoteColumn(
            modifier = RemoteModifier.fillMaxWidth().fillMaxHeight(),
            verticalArrangement = RemoteArrangement.SpaceBetween,
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
                ) {
                    RemoteIcon(
                        modifier = RemoteModifier.size(16.rdp),
                        imageVector =
                            if (!hasLocation) RemoteImageVector.vectorResource(R.drawable.location_disabled_24px)
                            else RemoteImageVector.vectorResource(R.drawable.location_city_24px),
                        contentDescription = "Location Icon".rs,
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

            RemoteBox(
                modifier = RemoteModifier.fillMaxWidth().weight(1f),
                contentAlignment = RemoteAlignment.Center
            ) {
                SolarArcGraphCanvas(
                    hasLocation = hasLocation,
                    sunriseMinute = sunriseMinute,
                    sunsetMinute = sunsetMinute,
                    noonMinute = noonMinute
                )
                if (!hasLocation) {
                    RemoteText(
                        text = locationNotSetText.rs,
                        style = RemoteMaterialTheme.typography.bodySmall,
                        color = RemoteMaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RemoteRow(
                modifier = RemoteModifier.fillMaxWidth().padding(top = 2.rdp),
                horizontalArrangement = RemoteArrangement.SpaceBetween,
                verticalAlignment = RemoteAlignment.CenterVertically
            ) {
                RemoteRow(
                    verticalAlignment = RemoteAlignment.CenterVertically,
                    horizontalArrangement = RemoteArrangement.spacedBy(3.rdp)
                ) {
                    RemoteIcon(
                        modifier = RemoteModifier.size(14.rdp),
                        imageVector = RemoteImageVector.vectorResource(R.drawable.ic_dawn),
                        contentDescription = sunriseTitle.rs,
                        tint = RemoteMaterialTheme.colorScheme.tertiary
                    )
                    RemoteText(
                        text = sunriseTime.rs,
                        style = RemoteMaterialTheme.typography.bodySmall,
                        color = RemoteMaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                RemoteRow(
                    verticalAlignment = RemoteAlignment.CenterVertically,
                    horizontalArrangement = RemoteArrangement.spacedBy(3.rdp)
                ) {
                    RemoteText(
                        text = sunsetTime.rs,
                        style = RemoteMaterialTheme.typography.bodySmall,
                        color = RemoteMaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    RemoteIcon(
                        modifier = RemoteModifier.size(14.rdp),
                        imageVector = RemoteImageVector.vectorResource(R.drawable.ic_dusk),
                        contentDescription = sunsetTitle.rs,
                        tint = RemoteMaterialTheme.colorScheme.primaryDim
                    )
                }
            }
        }
    }
    // Only Nicer Click effect
    RemoteCard(
        colors = RemoteCardDefaults.cardColors(containerColor = RemoteColor(Color.TRANSPARENT)),
        modifier = RemoteModifier.fillMaxSize(),
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
    ) {}
}

@SuppressLint("RestrictedApi")
@RemoteComposable
@Composable
private fun SolarArcGraphCanvas(
    hasLocation: Boolean,
    sunriseMinute: Float,
    sunsetMinute: Float,
    @Suppress("UNUSED_PARAMETER") noonMinute: Float
) {
    val colorScheme = RemoteMaterialTheme.colorScheme
    val containerColor = RemoteCardDefaults.cardColors().containerColor

    val hasValidSolar = hasLocation && sunriseMinute >= 0f && sunsetMinute > sunriseMinute
    val srMin = if (hasValidSolar) sunriseMinute else 360f
    val ssMin = if (hasValidSolar) sunsetMinute else 1080f
    val centerMin = (srMin + ssMin) / 2f
    val halfDayMin = (ssMin - srMin) / 2f

    val twoPiOverDay = (2.0 * PI / 1440.0).toFloat()
    val horizonElevation = mathCos((halfDayMin * twoPiOverDay).toDouble()).toFloat()
    val horizonV = (1f - horizonElevation) * 0.5f

    val currentMinute = RemoteTime().Minutes()
    val isDayNow = if (hasValidSolar) {
        currentMinute.isGreaterThanOrEqualTo(srMin.rf).and(currentMinute.isLessThanOrEqualTo(ssMin.rf))
    } else false.rb
    val isNightNow = if (hasValidSolar) {
        currentMinute.isLessThan(srMin.rf).or(currentMinute.isGreaterThan(ssMin.rf))
    } else false.rb

    val horizonPaint = RemotePaint {
        color = colorScheme.outlineVariant
        style = PaintingStyle.Stroke
        strokeWidth = 2f.rf
        strokeCap = StrokeCap.Round
    }
    val nightCurvePaint = RemotePaint {
        color = if (hasValidSolar) colorScheme.outline else colorScheme.outlineVariant
        style = PaintingStyle.Stroke
        strokeWidth = 4f.rf
        strokeCap = StrokeCap.Round
        strokeJoin = StrokeJoin.Round
    }
    val dayCurvePaint = RemotePaint {
        color = if (hasValidSolar) colorScheme.primary else colorScheme.outlineVariant
        style = PaintingStyle.Stroke
        strokeWidth = 5f.rf
        strokeCap = StrokeCap.Round
        strokeJoin = StrokeJoin.Round
    }
    val milestoneDotPaint = RemotePaint {
        color = colorScheme.primaryDim
        style = PaintingStyle.Fill
    }
    val sunGlowPaint = RemotePaint {
        color = colorScheme.tertiaryDim
        style = PaintingStyle.Fill
    }
    val sunCutoutPaint = RemotePaint {
        color = containerColor
        style = PaintingStyle.Fill
    }
    val sunFillPaint = RemotePaint {
        color = isDayNow.select(colorScheme.tertiary, colorScheme.secondary)
        style = PaintingStyle.Fill
    }

    RemoteCanvas(
        modifier = RemoteModifier.fillMaxSize()
    ) {
        val padX = 10f.rf
        val topPad = 11f.rf
        val bottomPad = 11f.rf
        val graphW = width - (padX * 2f)
        val graphH = height - topPad - bottomPad
        val horizonY = topPad + (graphH * horizonV)

        // Horizontal horizon line
        drawLine(
            paint = horizonPaint,
            start = RemoteOffset(padX, horizonY),
            end = RemoteOffset(width - padX, horizonY)
        )

        fun addCosineCubic(path: RemotePath, t0: Float, t3: Float, moveToStart: Boolean) {
            val u0 = t0 / 1440f
            val u3 = t3 / 1440f
            val du = (u3 - u0) / 3f

            val theta0 = ((t0 - centerMin) * twoPiOverDay).toDouble()
            val theta3 = ((t3 - centerMin) * twoPiOverDay).toDouble()
            val deltaTheta = theta3 - theta0
            val tangentScale = ((4.0 / 3.0) * mathTan(deltaTheta / 4.0)).toFloat()

            val v0 = ((1.0 - mathCos(theta0)) * 0.5).toFloat()
            val v3 = ((1.0 - mathCos(theta3)) * 0.5).toFloat()
            val v1 = v0 + 0.5f * tangentScale * mathSin(theta0).toFloat()
            val v2 = v3 - 0.5f * tangentScale * mathSin(theta3).toFloat()

            if (moveToStart) {
                val x0 = padX + (graphW * u0)
                val y0 = topPad + (graphH * v0)
                path.moveTo(x0.floatId, y0.floatId)
            }

            val x1 = padX + (graphW * (u0 + du))
            val y1 = topPad + (graphH * v1)
            val x2 = padX + (graphW * (u3 - du))
            val y2 = topPad + (graphH * v2)
            val x3 = padX + (graphW * u3)
            val y3 = topPad + (graphH * v3)

            path.cubicTo(
                x1.floatId, y1.floatId,
                x2.floatId, y2.floatId,
                x3.floatId, y3.floatId
            )
        }

        // Night path (pre-dawn [0..srMin] and post-dusk [ssMin..1440])
        val nightPath = RemotePath()
        val preDawnMid = srMin * 0.5f
        addCosineCubic(nightPath, 0f, preDawnMid, moveToStart = true)
        addCosineCubic(nightPath, preDawnMid, srMin, moveToStart = false)

        val postDuskMid = (ssMin + 1440f) * 0.5f
        addCosineCubic(nightPath, ssMin, postDuskMid, moveToStart = true)
        addCosineCubic(nightPath, postDuskMid, 1440f, moveToStart = false)
        drawPath(path = nightPath, paint = nightCurvePaint)

        // Day path (morning [srMin..centerMin] and afternoon [centerMin..ssMin])
        val dayPath = RemotePath()
        addCosineCubic(dayPath, srMin, centerMin, moveToStart = true)
        addCosineCubic(dayPath, centerMin, ssMin, moveToStart = false)
        drawPath(path = dayPath, paint = dayCurvePaint)

        if (hasValidSolar) {
            // Horizon intersection dots at Sunrise and Sunset
            val sunriseX = padX + (graphW * (srMin / 1440f))
            val sunsetX = padX + (graphW * (ssMin / 1440f))
            drawCircle(
                paint = milestoneDotPaint,
                radius = 3.5f.rf,
                center = RemoteOffset(sunriseX, horizonY)
            )
            drawCircle(
                paint = milestoneDotPaint,
                radius = 3.5f.rf,
                center = RemoteOffset(sunsetX, horizonY)
            )

            // Live sun position marker along the 24-hour curve
            val clampedMinute = clamp(currentMinute, 0f.rf, 1440f.rf)
            val sunU = clampedMinute / 1440f
            val sunX = padX + (graphW * sunU)
            val sunPhase = (clampedMinute - centerMin) * twoPiOverDay
            val sunV = (1f.rf - cos(sunPhase)) * 0.5f
            val sunY = topPad + (graphH * sunV)
            val sunCenter = RemoteOffset(sunX, sunY)

            drawConditionally(isDayNow) {
                drawCircle(
                    paint = sunGlowPaint,
                    radius = 9.5f.rf,
                    center = sunCenter
                )
            }
            drawCircle(
                paint = sunCutoutPaint,
                radius = 7.5f.rf,
                center = sunCenter
            )
            drawCircle(
                paint = sunFillPaint,
                radius = 5.5f.rf,
                center = sunCenter
            )
            drawConditionally(isNightNow) {
                drawCircle(
                    paint = sunCutoutPaint,
                    radius = 3f.rf,
                    center = sunCenter
                )
            }
        }
    }
}
