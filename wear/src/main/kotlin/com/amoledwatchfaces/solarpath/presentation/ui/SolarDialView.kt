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
            // 0. Daylight = Background Layer
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

            // Outer ring border
            drawCircle(
                color = Color(0xFF37474F),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Horizon Line
            drawLine(
                color = Color(0xFF78909C),
                start = Offset(center.x - outerRadius, center.y),
                end = Offset(center.x + outerRadius, center.y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )

            // Timeline Circle (matching watch face inner circle)
            val timelineRadius = outerRadius * 0.51f
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = timelineRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Timeline Dots for all solar events
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
                    color = Color.White.copy(alpha = 0.5f),
                    radius = 2.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }

            // Sun marker position on timeline circle
            val sunRad = Math.toRadians((sunAngleDeg - 90.0))
            val sunX = center.x + (timelineRadius * cos(sunRad)).toFloat()
            val sunY = center.y + (timelineRadius * sin(sunRad)).toFloat()

            // Ray to center
            drawLine(
                color = Color(0xFFFFE082).copy(alpha = 0.55f),
                start = center,
                end = Offset(sunX, sunY),
                strokeWidth = 1.5.dp.toPx()
            )

            // Corona
            drawCircle(
                color = Color(0xFFFFB300).copy(alpha = 0.35f),
                radius = 8.dp.toPx(),
                center = Offset(sunX, sunY)
            )

            // Sun Core Disc
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 4.dp.toPx(),
                center = Offset(sunX, sunY)
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(sunX, sunY),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Center Pin
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 2.5.dp.toPx(),
                center = center
            )
        }
    }
}
