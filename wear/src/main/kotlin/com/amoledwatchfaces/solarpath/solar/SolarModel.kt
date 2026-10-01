package com.amoledwatchfaces.solarpath.solar

data class SolarData(
    val sunriseEpoch: Long = 0L,
    val sunsetEpoch: Long = 0L,
    val solarNoonEpoch: Long = 0L,
    val solarNadirEpoch: Long = 0L,
    val civilDawnEpoch: Long = 0L,
    val civilDuskEpoch: Long = 0L,
    val nauticalDawnEpoch: Long = 0L,
    val nauticalDuskEpoch: Long = 0L,
    val astroDawnEpoch: Long = 0L,
    val astroDuskEpoch: Long = 0L,
    val daylightDurationMinutes: Long = 0L,
    val sunElevation: Double = 0.0,
    val sunAzimuth: Double = 0.0,
    val isDay: Boolean = false,
    val nextEventName: String = "Sunrise",
    val nextEventEpoch: Long = 0L,
    val progressFraction: Float = 0f,
    // Exact 24-hour dial angles in degrees (0..360, 0 = Solar Noon / 12:00 at top)
    val sunriseAngle: Float = 270f,
    val sunsetAngle: Float = 90f,
    val civilDawnAngle: Float = 255f,
    val civilDuskAngle: Float = 105f,
    val nauticalDawnAngle: Float = 240f,
    val nauticalDuskAngle: Float = 120f,
    val astroDawnAngle: Float = 225f,
    val astroDuskAngle: Float = 135f,
    val solarNoonAngle: Float = 0f,
    val solarMidnightAngle: Float = 180f
)

enum class SolarPhase {
    DAYLIGHT,
    CIVIL_TWILIGHT,
    NAUTICAL_TWILIGHT,
    ASTRONOMICAL_TWILIGHT,
    NIGHT
}
