package com.amoledwatchfaces.solarpath.solar

import dev.jamesyox.kastro.sol.SolarEvent
import dev.jamesyox.kastro.sol.SolarEventSequence
import dev.jamesyox.kastro.sol.calculateSolarState
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

object SolarCalculator {

    fun calculateSolarData(
        lat: Double,
        lon: Double,
        date: LocalDate = LocalDate.now(),
        currentTimeMillis: Long = System.currentTimeMillis()
    ): SolarData {
        if (lat == 0.0 && lon == 0.0) {
            return SolarData()
        }

        val startInstant = Instant.fromEpochMilliseconds(
            date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        val requestedEvents = listOf(
            SolarEvent.Sunrise,
            SolarEvent.Sunset,
            SolarEvent.Noon,
            SolarEvent.Nadir,
            SolarEvent.CivilDawn,
            SolarEvent.CivilDusk,
            SolarEvent.NauticalDawn,
            SolarEvent.NauticalDusk,
            SolarEvent.AstronomicalDawn,
            SolarEvent.AstronomicalDusk
        )

        val eventsSequence = SolarEventSequence(
            start = startInstant,
            latitude = lat,
            longitude = lon,
            requestedSolarEvents = requestedEvents,
            limit = 2.days
        )

        val eventList = eventsSequence.toList()

        val sunrise = eventList.find { it is SolarEvent.Sunrise }?.time?.toEpochMilliseconds() ?: 0L
        val sunset = eventList.find { it is SolarEvent.Sunset }?.time?.toEpochMilliseconds() ?: 0L
        val noon = eventList.find { it is SolarEvent.Noon }?.time?.toEpochMilliseconds() ?: 0L
        val nadir = eventList.find { it is SolarEvent.Nadir }?.time?.toEpochMilliseconds() ?: 0L

        val civilDawn = eventList.find { it is SolarEvent.CivilDawn }?.time?.toEpochMilliseconds() ?: 0L
        val civilDusk = eventList.find { it is SolarEvent.CivilDusk }?.time?.toEpochMilliseconds() ?: 0L
        val nauticalDawn = eventList.find { it is SolarEvent.NauticalDawn }?.time?.toEpochMilliseconds() ?: 0L
        val nauticalDusk = eventList.find { it is SolarEvent.NauticalDusk }?.time?.toEpochMilliseconds() ?: 0L
        val astroDawn = eventList.find { it is SolarEvent.AstronomicalDawn }?.time?.toEpochMilliseconds() ?: 0L
        val astroDusk = eventList.find { it is SolarEvent.AstronomicalDusk }?.time?.toEpochMilliseconds() ?: 0L

        val currentInstant = Instant.fromEpochMilliseconds(currentTimeMillis)
        val solarState = currentInstant.calculateSolarState(latitude = lat, longitude = lon)

        val isDay = solarState.trueAltitude > 0.0

        val daylightMinutes = if (sunset > sunrise && sunrise > 0) {
            (sunset - sunrise) / (60 * 1000)
        } else 0L

        // Next upcoming event
        val upcomingEvents = listOf(
            "Sunrise" to sunrise,
            "Solar Noon" to noon,
            "Sunset" to sunset,
            "Civil Dusk" to civilDusk,
            "Nautical Dusk" to nauticalDusk,
            "Astro Dusk" to astroDusk,
            "Solar Midnight" to nadir,
            "Astro Dawn" to astroDawn,
            "Nautical Dawn" to nauticalDawn,
            "Civil Dawn" to civilDawn
        ).filter { it.second > currentTimeMillis }.sortedBy { it.second }

        val nextEvent = upcomingEvents.firstOrNull() ?: ("Sunrise" to sunrise)

        // Daylight progress fraction (0..1)
        val progress = if (isDay && sunset > sunrise) {
            ((currentTimeMillis - sunrise).toFloat() / (sunset - sunrise).toFloat()).coerceIn(0f, 1f)
        } else if (!isDay && sunrise > 0) {
            0f
        } else 0f

        val zone = ZoneId.systemDefault()
        val srAngle = if (sunrise > 0) epochToDialAngle(sunrise, zone) else 270f
        val ssAngle = if (sunset > 0) epochToDialAngle(sunset, zone) else 90f
        val cdawnAngle = if (civilDawn > 0) epochToDialAngle(civilDawn, zone) else 255f
        val cduskAngle = if (civilDusk > 0) epochToDialAngle(civilDusk, zone) else 105f
        val ndawnAngle = if (nauticalDawn > 0) epochToDialAngle(nauticalDawn, zone) else 240f
        val nduskAngle = if (nauticalDusk > 0) epochToDialAngle(nauticalDusk, zone) else 120f
        val adawnAngle = if (astroDawn > 0) epochToDialAngle(astroDawn, zone) else 225f
        val aduskAngle = if (astroDusk > 0) epochToDialAngle(astroDusk, zone) else 135f

        return SolarData(
            sunriseEpoch = sunrise,
            sunsetEpoch = sunset,
            solarNoonEpoch = noon,
            solarNadirEpoch = nadir,
            civilDawnEpoch = civilDawn,
            civilDuskEpoch = civilDusk,
            nauticalDawnEpoch = nauticalDawn,
            nauticalDuskEpoch = nauticalDusk,
            astroDawnEpoch = astroDawn,
            astroDuskEpoch = astroDusk,
            daylightDurationMinutes = daylightMinutes,
            sunElevation = solarState.trueAltitude,
            sunAzimuth = solarState.azimuth,
            isDay = isDay,
            nextEventName = nextEvent.first,
            nextEventEpoch = nextEvent.second,
            progressFraction = progress,
            sunriseAngle = srAngle,
            sunsetAngle = ssAngle,
            civilDawnAngle = cdawnAngle,
            civilDuskAngle = cduskAngle,
            nauticalDawnAngle = ndawnAngle,
            nauticalDuskAngle = nduskAngle,
            astroDawnAngle = adawnAngle,
            astroDuskAngle = aduskAngle
        )
    }

    fun epochToDialAngle(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Float {
        if (epochMillis <= 0L) return 0f
        val localTime = java.time.Instant.ofEpochMilli(epochMillis)
            .atZone(zoneId)
            .toLocalTime()
        val hourDecimal = localTime.hour + localTime.minute / 60.0 + localTime.second / 3600.0
        var angle = ((hourDecimal + 12.0) * 15.0) % 360.0
        if (angle < 0.0) angle += 360.0
        return angle.toFloat()
    }

    fun formatAnglesForComplication(solar: SolarData, nextEventName: String): String {
        val ss = (solar.sunsetAngle.toInt() % 360).let { if (it < 0) it + 360 else it }
        val cdusk = (solar.civilDuskAngle.toInt() % 360).let { if (it < 0) it + 360 else it }
        val ndusk = (solar.nauticalDuskAngle.toInt() % 360).let { if (it < 0) it + 360 else it }
        val adusk = (solar.astroDuskAngle.toInt() % 360).let { if (it < 0) it + 360 else it }
        val adawn = (solar.astroDawnAngle.toInt() % 360).let { if (it < 0) it + 360 else it }
        val ndawn = (solar.nauticalDawnAngle.toInt() % 360).let { if (it < 0) it + 360 else it }
        val cdawn = (solar.civilDawnAngle.toInt() % 360).let { if (it < 0) it + 360 else it }
        val sr = (solar.sunriseAngle.toInt() % 360).let { if (it < 0) it + 360 else it }

        return "\uFEFF%03d%03d%03d%03d%03d%03d%03d%03d %s".format(
            ss, cdusk, ndusk, adusk, adawn, ndawn, cdawn, sr, nextEventName.uppercase()
        )
    }

    fun getSolarPhase(elevationDegrees: Double): SolarPhase {
        return when {
            elevationDegrees > 0.0 -> SolarPhase.DAYLIGHT
            elevationDegrees > -6.0 -> SolarPhase.CIVIL_TWILIGHT
            elevationDegrees > -12.0 -> SolarPhase.NAUTICAL_TWILIGHT
            elevationDegrees > -18.0 -> SolarPhase.ASTRONOMICAL_TWILIGHT
            else -> SolarPhase.NIGHT
        }
    }
}
