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
import androidx.compose.ui.graphics.StrokeCap
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
            val outerRadius = size.minDimension / 2f - 8.dp.toPx()
            val ringThickness = 14.dp.toPx()
            val arcSize = Size(outerRadius * 2, outerRadius * 2)
            val arcTopLeft = Offset(center.x - outerRadius, center.y - outerRadius)

            fun toCanvas(dialAngle: Float): Float = (dialAngle - 90f + 360f) % 360f
            fun sweep(start: Float, end: Float): Float {
                var s = (end - start) % 360f
                if (s <= 0f) s += 360f
                return s
            }

            // 1. Full Night (deep midnight charcoal, distinct from OLED black)
            drawArc(
                color = Color(0xFF151B26),
                startAngle = toCanvas(solarData.astroDuskAngle),
                sweepAngle = sweep(solarData.astroDuskAngle, solarData.astroDawnAngle),
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = ringThickness, cap = StrokeCap.Butt)
            )

            // 2. Astronomical Twilight Dusk (deep navy)
            drawArc(
                color = Color(0xFF1A237E),
                startAngle = toCanvas(solarData.nauticalDuskAngle),
                sweepAngle = sweep(solarData.nauticalDuskAngle, solarData.astroDuskAngle),
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = ringThickness, cap = StrokeCap.Butt)
            )

            // 3. Astronomical Twilight Dawn (deep navy)
            drawArc(
                color = Color(0xFF1A237E),
                startAngle = toCanvas(solarData.astroDawnAngle),
                sweepAngle = sweep(solarData.astroDawnAngle, solarData.nauticalDawnAngle),
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = ringThickness, cap = StrokeCap.Butt)
            )

            // 4. Nautical Twilight Dusk (rich indigo)
            drawArc(
                color = Color(0xFF283593),
                startAngle = toCanvas(solarData.civilDuskAngle),
                sweepAngle = sweep(solarData.civilDuskAngle, solarData.nauticalDuskAngle),
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = ringThickness, cap = StrokeCap.Butt)
            )

            // 5. Nautical Twilight Dawn (rich indigo)
            drawArc(
                color = Color(0xFF283593),
                startAngle = toCanvas(solarData.nauticalDawnAngle),
                sweepAngle = sweep(solarData.nauticalDawnAngle, solarData.civilDawnAngle),
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = ringThickness, cap = StrokeCap.Butt)
            )

            // 6. Civil Twilight Dusk
            drawArc(
                color = Color(0xFFE65100),
                startAngle = toCanvas(solarData.sunsetAngle),
                sweepAngle = sweep(solarData.sunsetAngle, solarData.civilDuskAngle),
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = ringThickness, cap = StrokeCap.Butt)
            )

            // 7. Civil Twilight Dawn
            drawArc(
                color = Color(0xFFE65100),
                startAngle = toCanvas(solarData.civilDawnAngle),
                sweepAngle = sweep(solarData.civilDawnAngle, solarData.sunriseAngle),
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = ringThickness, cap = StrokeCap.Butt)
            )

            // 8. Daylight (vibrant solar sky blue)
            drawArc(
                color = Color(0xFF1976D2),
                startAngle = toCanvas(solarData.sunriseAngle),
                sweepAngle = sweep(solarData.sunriseAngle, solarData.sunsetAngle),
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = ringThickness, cap = StrokeCap.Butt)
            )

            // Horizon Line
            drawLine(
                color = Color(0xFF78909C),
                start = Offset(center.x - outerRadius - 6.dp.toPx(), center.y),
                end = Offset(center.x + outerRadius + 6.dp.toPx(), center.y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            // Inner subdial circle
            val innerRadius = outerRadius - ringThickness - 4.dp.toPx()
            drawCircle(
                color = Color(0xFF0D1117),
                radius = innerRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFF30363D),
                radius = innerRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Sun marker position on ring
            val angleRad = Math.toRadians((sunAngleDeg - 90.0))
            val sunRadius = outerRadius
            val sunX = center.x + (sunRadius * cos(angleRad)).toFloat()
            val sunY = center.y + (sunRadius * sin(angleRad)).toFloat()

            // Ray to center
            drawLine(
                color = Color(0xFFFFE082).copy(alpha = 0.5f),
                start = center,
                end = Offset(sunX, sunY),
                strokeWidth = 1.dp.toPx()
            )

            // Corona
            drawCircle(
                color = Color(0xFFFFB300).copy(alpha = 0.35f),
                radius = 10.dp.toPx(),
                center = Offset(sunX, sunY)
            )

            // Sun Core
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 5.dp.toPx(),
                center = Offset(sunX, sunY)
            )
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(sunX, sunY),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Center Pin
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 3.dp.toPx(),
                center = center
            )
        }
    }
}
