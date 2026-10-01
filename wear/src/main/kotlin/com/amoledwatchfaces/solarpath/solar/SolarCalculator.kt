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
            progressFraction = progress
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
