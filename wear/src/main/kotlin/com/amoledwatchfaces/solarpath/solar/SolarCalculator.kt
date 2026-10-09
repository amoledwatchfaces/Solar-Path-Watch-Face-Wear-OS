package com.amoledwatchfaces.solarpath.solar

import dev.jamesyox.kastro.sol.SolarEvent
import dev.jamesyox.kastro.sol.SolarEventSequence
import dev.jamesyox.kastro.sol.calculateSolarState
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

data class DailySolarEvents(
    val date: LocalDate,
    val lat: Double,
    val lon: Double,
    val zoneId: ZoneId = ZoneId.systemDefault(),
    val sunrise: Long,
    val sunset: Long,
    val noon: Long,
    val nadir: Long,
    val civilDawn: Long,
    val civilDusk: Long,
    val nauticalDawn: Long,
    val nauticalDusk: Long,
    val astroDawn: Long,
    val astroDusk: Long,
    val daylightMinutes: Long,
    val sunriseAngle: Float,
    val sunsetAngle: Float,
    val civilDawnAngle: Float,
    val civilDuskAngle: Float,
    val nauticalDawnAngle: Float,
    val nauticalDuskAngle: Float,
    val astroDawnAngle: Float,
    val astroDuskAngle: Float,
    val noonAngle: Float,
    val nadirAngle: Float
)

object SolarCalculator {

    private data class CacheKey(
        val date: LocalDate,
        val zoneId: ZoneId,
        val latE4: Long,
        val lonE4: Long
    )

    private val cache = java.util.concurrent.ConcurrentHashMap<CacheKey, DailySolarEvents>()

    fun invalidateCache() {
        cache.clear()
    }

    @Synchronized
    private fun getOrCalculateDailyEvents(
        lat: Double,
        lon: Double,
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DailySolarEvents {
        val key = CacheKey(
            date = date,
            zoneId = zoneId,
            latE4 = (lat * 10000).toLong(),
            lonE4 = (lon * 10000).toLong()
        )
        val cached = cache[key]
        if (cached != null) {
            return cached
        }

        val startInstant = Instant.fromEpochMilliseconds(
            date.atStartOfDay(zoneId).toInstant().toEpochMilli()
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
            SolarEvent.AstronomicalDusk,
            SolarEvent.Night
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

        // In Kastro (kotlin), dusk events mark the transitions:
        // - Sunset begins Civil Dusk (sun at 0°)
        // - NauticalDusk marks sun reaching -6° (end of Civil Dusk, start of Nautical Dusk)
        // - AstronomicalDusk marks sun reaching -12° (end of Nautical Dusk, start of Astro Dusk)
        // - Night marks sun reaching -18° (end of Astro Dusk, start of Night)
        val civilDusk = eventList.find { it is SolarEvent.NauticalDusk }?.time?.toEpochMilliseconds() ?: 0L
        val nauticalDusk = eventList.find { it is SolarEvent.AstronomicalDusk }?.time?.toEpochMilliseconds() ?: 0L
        val astroDusk = eventList.find { it is SolarEvent.Night }?.time?.toEpochMilliseconds() ?: 0L

        // Dawn transitions:
        // - AstronomicalDawn marks sun reaching -18° (end of Night, start of Astro Dawn)
        // - NauticalDawn marks sun reaching -12° (end of Astro Dawn, start of Nautical Dawn)
        // - CivilDawn marks sun reaching -6° (end of Nautical Dawn, start of Civil Dawn)
        // - Sunrise marks sun reaching 0° (end of Civil Dawn, start of Day)
        val astroDawn = eventList.find { it is SolarEvent.AstronomicalDawn }?.time?.toEpochMilliseconds() ?: 0L
        val nauticalDawn = eventList.find { it is SolarEvent.NauticalDawn }?.time?.toEpochMilliseconds() ?: 0L
        val civilDawn = eventList.find { it is SolarEvent.CivilDawn }?.time?.toEpochMilliseconds() ?: 0L

        val daylightMinutes = if (sunset > sunrise && sunrise > 0) {
            (sunset - sunrise) / (60 * 1000)
        } else 0L

        val srAngle = if (sunrise > 0) epochToDialAngle(sunrise, zoneId) else 270f
        val ssAngle = if (sunset > 0) epochToDialAngle(sunset, zoneId) else 90f
        val cdawnAngle = if (civilDawn > 0) epochToDialAngle(civilDawn, zoneId) else 255f
        val cduskAngle = if (civilDusk > 0) epochToDialAngle(civilDusk, zoneId) else 105f
        val ndawnAngle = if (nauticalDawn > 0) epochToDialAngle(nauticalDawn, zoneId) else 240f
        val nduskAngle = if (nauticalDusk > 0) epochToDialAngle(nauticalDusk, zoneId) else 120f
        val adawnAngle = if (astroDawn > 0) epochToDialAngle(astroDawn, zoneId) else 225f
        val aduskAngle = if (astroDusk > 0) epochToDialAngle(astroDusk, zoneId) else 135f
        val noonAngle = if (noon > 0) epochToDialAngle(noon, zoneId) else 0f
        val nadirAngle = if (nadir > 0) epochToDialAngle(nadir, zoneId) else 180f

        val calculated = DailySolarEvents(
            date = date,
            lat = lat,
            lon = lon,
            zoneId = zoneId,
            sunrise = sunrise,
            sunset = sunset,
            noon = noon,
            nadir = nadir,
            civilDawn = civilDawn,
            civilDusk = civilDusk,
            nauticalDawn = nauticalDawn,
            nauticalDusk = nauticalDusk,
            astroDawn = astroDawn,
            astroDusk = astroDusk,
            daylightMinutes = daylightMinutes,
            sunriseAngle = srAngle,
            sunsetAngle = ssAngle,
            civilDawnAngle = cdawnAngle,
            civilDuskAngle = cduskAngle,
            nauticalDawnAngle = ndawnAngle,
            nauticalDuskAngle = nduskAngle,
            astroDawnAngle = adawnAngle,
            astroDuskAngle = aduskAngle,
            noonAngle = noonAngle,
            nadirAngle = nadirAngle
        )
        cache[key] = calculated
        return calculated
    }

    fun calculateSolarData(
        lat: Double,
        lon: Double,
        date: LocalDate = LocalDate.now(),
        currentTimeMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): SolarData {
        if (lat == 0.0 && lon == 0.0) {
            return SolarData()
        }

        val daily = getOrCalculateDailyEvents(lat, lon, date, zoneId)
        val dailyTomorrow = getOrCalculateDailyEvents(lat, lon, date.plusDays(1), zoneId)

        val effectiveTimeMillis = if (currentTimeMillis <= 0L) {
            date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        } else {
            currentTimeMillis
        }

        val currentInstant = Instant.fromEpochMilliseconds(effectiveTimeMillis)
        val solarState = currentInstant.calculateSolarState(latitude = lat, longitude = lon)
        val isDay = solarState.trueAltitude > 0.0

        val todayEvents = listOf(
            "Sunrise" to daily.sunrise,
            "Solar Noon" to daily.noon,
            "Sunset" to daily.sunset,
            "Civil Dusk" to daily.civilDusk,
            "Nautical Dusk" to daily.nauticalDusk,
            "Astro Dusk" to daily.astroDusk,
            "Solar Midnight" to daily.nadir,
            "Astro Dawn" to daily.astroDawn,
            "Nautical Dawn" to daily.nauticalDawn,
            "Civil Dawn" to daily.civilDawn
        )

        val tomorrowEvents = listOf(
            "Sunrise" to dailyTomorrow.sunrise,
            "Solar Noon" to dailyTomorrow.noon,
            "Sunset" to dailyTomorrow.sunset,
            "Civil Dusk" to dailyTomorrow.civilDusk,
            "Nautical Dusk" to dailyTomorrow.nauticalDusk,
            "Astro Dusk" to dailyTomorrow.astroDusk,
            "Solar Midnight" to dailyTomorrow.nadir,
            "Astro Dawn" to dailyTomorrow.astroDawn,
            "Nautical Dawn" to dailyTomorrow.nauticalDawn,
            "Civil Dawn" to dailyTomorrow.civilDawn
        )

        val upcomingEvents = (todayEvents + tomorrowEvents)
            .filter { it.second > effectiveTimeMillis }
            .sortedBy { it.second }

        val nextEvent = upcomingEvents.firstOrNull() ?: (
            "Sunrise" to if (daily.sunrise > 0 && daily.sunrise < effectiveTimeMillis) daily.sunrise + 86_400_000L else daily.sunrise
        )

        // Daylight progress fraction (0..1)
        val progress = if (isDay && daily.sunset > daily.sunrise) {
            ((currentTimeMillis - daily.sunrise).toFloat() / (daily.sunset - daily.sunrise).toFloat()).coerceIn(0f, 1f)
        } else 0f

        return SolarData(
            sunriseEpoch = daily.sunrise,
            sunsetEpoch = daily.sunset,
            solarNoonEpoch = daily.noon,
            solarNadirEpoch = daily.nadir,
            civilDawnEpoch = daily.civilDawn,
            civilDuskEpoch = daily.civilDusk,
            nauticalDawnEpoch = daily.nauticalDawn,
            nauticalDuskEpoch = daily.nauticalDusk,
            astroDawnEpoch = daily.astroDawn,
            astroDuskEpoch = daily.astroDusk,
            daylightDurationMinutes = daily.daylightMinutes,
            sunElevation = solarState.trueAltitude,
            sunAzimuth = solarState.azimuth,
            isDay = isDay,
            nextEventName = nextEvent.first,
            nextEventEpoch = nextEvent.second,
            progressFraction = progress,
            sunriseAngle = daily.sunriseAngle,
            sunsetAngle = daily.sunsetAngle,
            civilDawnAngle = daily.civilDawnAngle,
            civilDuskAngle = daily.civilDuskAngle,
            nauticalDawnAngle = daily.nauticalDawnAngle,
            nauticalDuskAngle = daily.nauticalDuskAngle,
            astroDawnAngle = daily.astroDawnAngle,
            astroDuskAngle = daily.astroDuskAngle,
            solarNoonAngle = daily.noonAngle,
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
        val noon = (solar.solarNoonAngle.toInt() % 360).let { if (it < 0) it + 360 else it }
        val midnight = (solar.solarMidnightAngle.toInt() % 360).let { if (it < 0) it + 360 else it }

        return "\uFEFF%03d%03d%03d%03d%03d%03d%03d%03d%03d%03d %s".format(
            ss, cdusk, ndusk, adusk, adawn, ndawn, cdawn, sr, noon, midnight, nextEventName.uppercase()
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
