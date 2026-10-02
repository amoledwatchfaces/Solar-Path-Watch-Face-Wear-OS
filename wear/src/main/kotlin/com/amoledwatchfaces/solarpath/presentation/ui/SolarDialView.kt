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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
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
    // Solar rotation angle: noon at top (0 deg / 270 deg in Canvas), 15 deg per hour
    val sunAngleDeg = ((currentHourDecimal + 12f) * 15f) % 360f

    // Material 3 theme colors matching watchface.xml
    val dayColor = MaterialTheme.colorScheme.tertiary.copy(0.7f)
    val civilColor = MaterialTheme.colorScheme.onSurfaceVariant
    val nauticalColor = MaterialTheme.colorScheme.outline
    val astroColor = MaterialTheme.colorScheme.outlineVariant
    val nightColor = MaterialTheme.colorScheme.surfaceContainerLow
    val onSurfaceColor = MaterialTheme.colorScheme.outlineVariant

    // Check if current time is day (between sunrise and sunset through noon)
    val isDay = if (solarData.sunriseAngle <= solarData.sunsetAngle) {
        sunAngleDeg >= solarData.sunriseAngle && sunAngleDeg <= solarData.sunsetAngle
    } else {
        sunAngleDeg >= solarData.sunriseAngle || sunAngleDeg <= solarData.sunsetAngle
    }

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

            // 1. Base Daytime Sky Circle (matching circle_base in watchface.xml)
            drawCircle(
                color = dayColor,
                radius = outerRadius,
                center = center
            )

            // 2. Stacked Twilight & Night Layers (matching watchface.xml <PartDraw name="layers">)
            // Layer 1: Civil Twilight (Sunset -> Sunrise)
            drawArc(
                color = civilColor,
                startAngle = toCanvas(solarData.sunsetAngle),
                sweepAngle = sweep(solarData.sunsetAngle, solarData.sunriseAngle),
                useCenter = true,
                topLeft = arcTopLeft,
                size = arcSize
            )

            // Layer 2: Nautical Twilight (Civil Dusk -> Civil Dawn)
            drawArc(
                color = nauticalColor,
                startAngle = toCanvas(solarData.civilDuskAngle),
                sweepAngle = sweep(solarData.civilDuskAngle, solarData.civilDawnAngle),
                useCenter = true,
                topLeft = arcTopLeft,
                size = arcSize
            )

            // Layer 3: Astronomical Twilight (Nautical Dusk -> Nautical Dawn)
            drawArc(
                color = astroColor,
                startAngle = toCanvas(solarData.nauticalDuskAngle),
                sweepAngle = sweep(solarData.nauticalDuskAngle, solarData.nauticalDawnAngle),
                useCenter = true,
                topLeft = arcTopLeft,
                size = arcSize
            )

            // Layer 4: Full Night (Astro Dusk -> Astro Dawn)
            drawArc(
                color = nightColor,
                startAngle = toCanvas(solarData.astroDuskAngle),
                sweepAngle = sweep(solarData.astroDuskAngle, solarData.astroDawnAngle),
                useCenter = true,
                topLeft = arcTopLeft,
                size = arcSize
            )

            // 3. Stars on the night sky (matching watchface.xml stars layer in bottom night sector)
            val starOffsets = listOf(
                Offset(0.0f, 0.70f),
                Offset(-0.35f, 0.55f),
                Offset(0.35f, 0.58f),
                Offset(-0.55f, 0.35f),
                Offset(0.55f, 0.32f),
                Offset(-0.20f, 0.42f),
                Offset(0.20f, 0.45f),
                Offset(-0.42f, 0.68f),
                Offset(0.42f, 0.50f),
                Offset(-0.15f, 0.62f),
                Offset(0.15f, 0.65f)
            )
            starOffsets.forEach { relOffset ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = 1.2.dp.toPx(),
                    center = center + Offset(relOffset.x * outerRadius, relOffset.y * outerRadius)
                )
            }

            // 4. Subtle Outer Border
            drawCircle(
                color = onSurfaceColor,
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 5. Timeline Circle (matching watchface.xml: radius ~0.50 of dial, alpha 115)
            val timelineRadius = outerRadius * 0.50f
            drawCircle(
                color = onSurfaceColor,
                radius = timelineRadius,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )

            // 6. Timeline Event Dots for solar events (matching watchface.xml dots)
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
                    color = onSurfaceColor,
                    radius = 2.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }

            // 7. Sun Beam Line (matching watchface.xml sun beam, alpha 75)
            val sunRad = Math.toRadians((sunAngleDeg - 90.0))
            val sunX = center.x + (timelineRadius * cos(sunRad)).toFloat()
            val sunY = center.y + (timelineRadius * sin(sunRad)).toFloat()

            // 8. Sun Disc Marker (matching watchface.xml: filled white during day, hollow at night)
            val sunOuterRadius = outerRadius * 0.08f
            val sunInnerRadius = outerRadius * 0.06f

            // Outer white ring
            drawCircle(
                color = Color.White,
                radius = sunOuterRadius,
                center = Offset(sunX, sunY)
            )

            // Inner circle: solid white during day, dark hollow at night
            val innerColor = if (isDay) Color.White else nightColor
            drawCircle(
                color = innerColor,
                radius = sunInnerRadius,
                center = Offset(sunX, sunY)
            )
        }
    }
}
