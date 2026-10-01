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
    val progressFraction: Float = 0f
)

enum class SolarPhase {
    DAYLIGHT,
    CIVIL_TWILIGHT,
    NAUTICAL_TWILIGHT,
    ASTRONOMICAL_TWILIGHT,
    NIGHT
}
