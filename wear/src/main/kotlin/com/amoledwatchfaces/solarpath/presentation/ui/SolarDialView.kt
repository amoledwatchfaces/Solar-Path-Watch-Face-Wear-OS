package com.amoledwatchfaces.solarpath.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.amoledwatchfaces.solarpath.solar.SolarData
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SolarDialView(
    solarData: SolarData,
    modifier: Modifier = Modifier
) {
    val currentTime = LocalTime.now()
    val currentHourDecimal = currentTime.hour + currentTime.minute / 60f + currentTime.second / 3600f
    // Solar rotation angle: noon at top (0 deg / -90 in Canvas), 15 deg per hour
    val sunAngleDeg = ((currentHourDecimal + 12f) * 15f) % 360f

    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f - 4.dp.toPx()
            val arcSize = Size(outerRadius * 2, outerRadius * 2)
            val arcTopLeft = Offset(center.x - outerRadius, center.y - outerRadius)

            fun toCanvas(dialAngle: Float): Float = (dialAngle - 90f + 360f) % 360f
            fun sweep(start: Float, end: Float): Float {
                var s = (end - start) % 360f
                if (s <= 0f) s += 360f
                return s
            }

            // Stacked twilight layers matching watchface.xml:
            // 0. Base Daylight Layer
            drawCircle(
                color = Color(0xFF1976D2),
                radius = outerRadius,
                center = center
            )

            // 1. Civil Twilight Layer: Sunset -> Sunrise
            drawArc(
                color = Color(0xFF3C4CC5),
                startAngle = toCanvas(solarData.sunsetAngle),
                sweepAngle = sweep(solarData.sunsetAngle, solarData.sunriseAngle),
                useCenter = true,
                topLeft = arcTopLeft,
                size = arcSize
            )

            // 2. Nautical Twilight Layer: Civil Dusk (-6°) -> Civil Dawn (-6°)
            drawArc(
                color = Color(0xFF283593),
                startAngle = toCanvas(solarData.civilDuskAngle),
                sweepAngle = sweep(solarData.civilDuskAngle, solarData.civilDawnAngle),
                useCenter = true,
                topLeft = arcTopLeft,
                size = arcSize
            )

            // 3. Astronomical Twilight Layer: Nautical Dusk (-12°) -> Nautical Dawn (-12°)
            drawArc(
                color = Color(0xFF1A237E),
                startAngle = toCanvas(solarData.nauticalDuskAngle),
                sweepAngle = sweep(solarData.nauticalDuskAngle, solarData.nauticalDawnAngle),
                useCenter = true,
                topLeft = arcTopLeft,
                size = arcSize
            )

            // 4. Full Night Layer: Astro Dusk (-18°) -> Astro Dawn (-18°) through Midnight 180°
            drawArc(
                color = Color(0xFF151B26),
                startAngle = toCanvas(solarData.astroDuskAngle),
                sweepAngle = sweep(solarData.astroDuskAngle, solarData.astroDawnAngle),
                useCenter = true,
                topLeft = arcTopLeft,
                size = arcSize
            )

            // Hour tick marks around dial rim (matching watchface.xml hour_index)
            for (hour in 0 until 12) {
                val tickAngle = hour * 30f // every 2 hours
                val tickRad = Math.toRadians((tickAngle - 90.0))
                val innerR = outerRadius - 5.dp.toPx()
                val startX = center.x + (innerR * cos(tickRad)).toFloat()
                val startY = center.y + (innerR * sin(tickRad)).toFloat()
                val endX = center.x + (outerRadius * cos(tickRad)).toFloat()
                val endY = center.y + (outerRadius * sin(tickRad)).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 1.2.dp.toPx()
                )
            }

            // Outer ring border
            drawCircle(
                color = Color(0xFF37474F),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Timeline Circle (matching watchface.xml: 230px on 450px face = radius factor 115/225 ≈ 0.511)
            val timelineRadius = outerRadius * (115f / 225f)
            drawCircle(
                color = Color.White.copy(alpha = 125f / 255f),
                radius = timelineRadius,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Timeline Dots for all 10 solar events (matching watchface.xml: 8px on 450px face, #60ffffff)
            val eventAngles = listOf(
                solarData.sunriseAngle,
                solarData.sunsetAngle,
                solarData.civilDawnAngle,
                solarData.civilDuskAngle,
                solarData.nauticalDawnAngle,
                solarData.nauticalDuskAngle,
                solarData.astroDawnAngle,
                solarData.astroDuskAngle,
                solarData.solarNoonAngle,
                solarData.solarMidnightAngle
            )
            eventAngles.forEach { angle ->
                val rad = Math.toRadians((angle - 90.0))
                val dotX = center.x + (timelineRadius * cos(rad)).toFloat()
                val dotY = center.y + (timelineRadius * sin(rad)).toFloat()
                drawCircle(
                    color = Color.White.copy(alpha = 0.38f),
                    radius = 2.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }

            // Sun marker position on timeline circle
            val sunRad = Math.toRadians((sunAngleDeg - 90.0))
            val sunX = center.x + (timelineRadius * cos(sunRad)).toFloat()
            val sunY = center.y + (timelineRadius * sin(sunRad)).toFloat()

            // Calculate Day vs Night state & transition factor for the sun disc
            // Disc diameter on 450 face is 30px => ~15° angular width (±7.5° radius)
            fun angularDist(a: Float, b: Float): Float {
                val diff = kotlin.math.abs(a - b) % 360f
                return if (diff > 180f) 360f - diff else diff
            }

            val isNightTime = if (solarData.sunsetAngle <= solarData.sunriseAngle) {
                sunAngleDeg >= solarData.sunsetAngle && sunAngleDeg <= solarData.sunriseAngle
            } else {
                sunAngleDeg >= solarData.sunsetAngle || sunAngleDeg <= solarData.sunriseAngle
            }

            // Smooth transition over the 7.5° disc radius around sunset / sunrise
            val transitionHalfWidth = 7.5f
            val distToSunset = angularDist(sunAngleDeg, solarData.sunsetAngle)
            val distToSunrise = angularDist(sunAngleDeg, solarData.sunriseAngle)
            val daylightFactor = when {
                distToSunset < transitionHalfWidth -> {
                    // Sunset: sunAngleDeg crossing from < sunset to > sunset
                    val signedOffset = (sunAngleDeg - solarData.sunsetAngle)
                    (0.5f - (signedOffset / (2f * transitionHalfWidth))).coerceIn(0f, 1f)
                }
                distToSunrise < transitionHalfWidth -> {
                    // Sunrise: sunAngleDeg crossing from < sunrise to > sunrise
                    val signedOffset = (sunAngleDeg - solarData.sunriseAngle)
                    (0.5f + (signedOffset / (2f * transitionHalfWidth))).coerceIn(0f, 1f)
                }
                isNightTime -> 0f
                else -> 1f
            }

            // Sun Beam Line (matching watchface.xml: from center outward past sun marker, alpha 75/255 ≈ 0.29)
            val beamEndR = outerRadius * (187f / 225f)
            val beamEndX = center.x + (beamEndR * cos(sunRad)).toFloat()
            val beamEndY = center.y + (beamEndR * sin(sunRad)).toFloat()
            drawLine(
                color = Color.White.copy(alpha = 75f / 255f),
                start = center,
                end = Offset(beamEndX, beamEndY),
                strokeWidth = 1.2.dp.toPx()
            )

            // Sun Halo (Radial gradient matching watchface.xml sun_halo, fades at night)
            if (daylightFactor > 0.01f) {
                val haloOuterRadius = 36.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFFFD).copy(alpha = 0.31f * daylightFactor),
                            Color.Transparent
                        ),
                        center = Offset(sunX, sunY),
                        radius = haloOuterRadius
                    ),
                    radius = haloOuterRadius,
                    center = Offset(sunX, sunY)
                )

                val haloCoreRadius = 14.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFFFD).copy(alpha = 0.65f * daylightFactor),
                            Color.Transparent
                        ),
                        center = Offset(sunX, sunY),
                        radius = haloCoreRadius
                    ),
                    radius = haloCoreRadius,
                    center = Offset(sunX, sunY)
                )
            }

            // Sun Core Disc (matching watchface.xml: 30px outer, 24px inner)
            // Outer ring: 30px on 450 face => ~5.5dp radius
            val sunOuterRadius = outerRadius * (15f / 225f)
            val sunInnerRadius = outerRadius * (12f / 225f)

            // Base white circle (provides crisp outline when inner is dark at night)
            drawCircle(
                color = Color(0xFFFFFFFD),
                radius = sunOuterRadius,
                center = Offset(sunX, sunY)
            )

            // Inner Disc: Solid white during day, dark #0D0D0D at night (Apple Solar Dial hollow marker)
            val innerColor = if (daylightFactor >= 0.99f) {
                Color(0xFFFFFFFD)
            } else {
                // Blend from white to #0D0D0D based on daylightFactor
                val r = (0xFF * daylightFactor + 0x0D * (1f - daylightFactor)).toInt()
                val g = (0xFF * daylightFactor + 0x0D * (1f - daylightFactor)).toInt()
                val b = (0xFD * daylightFactor + 0x0D * (1f - daylightFactor)).toInt()
                Color(android.graphics.Color.rgb(r, g, b))
            }
            drawCircle(
                color = innerColor,
                radius = sunInnerRadius,
                center = Offset(sunX, sunY)
            )
        }
    }
}
