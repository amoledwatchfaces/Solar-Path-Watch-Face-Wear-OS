package com.amoledwatchfaces.solarpath

import com.amoledwatchfaces.solarpath.solar.SolarCalculator
import org.junit.Test
import java.time.LocalDate

class SolarCalculatorTest {
    @Test
    fun testSolarAngles() {
        val lat = 48.1486 // Bratislava / Central Europe example
        val lon = 17.1077
        val data = SolarCalculator.calculateSolarData(lat, lon, LocalDate.now())
        println("=== SOLAR DATA DEBUG ===")
        println("Sunrise: ${data.sunriseEpoch} (angle=${data.sunriseAngle})")
        println("Sunset: ${data.sunsetEpoch} (angle=${data.sunsetAngle})")
        println("CivilDusk: ${data.civilDuskEpoch} (angle=${data.civilDuskAngle})")
        println("NauticalDusk: ${data.nauticalDuskEpoch} (angle=${data.nauticalDuskAngle})")
        println("AstroDusk: ${data.astroDuskEpoch} (angle=${data.astroDuskAngle})")
        println("AstroDawn: ${data.astroDawnEpoch} (angle=${data.astroDawnAngle})")
        println("NauticalDawn: ${data.nauticalDawnEpoch} (angle=${data.nauticalDawnAngle})")
        println("Noon: ${data.solarNoonEpoch} (angle=${data.solarNoonAngle})")
        println("Midnight: ${data.solarNadirEpoch} (angle=${data.solarMidnightAngle})")
        val formatted = SolarCalculator.formatAnglesForComplication(data, "SUNSET")
        println("Formatted string: $formatted")

        org.junit.Assert.assertTrue(data.civilDuskAngle > data.sunsetAngle)
        org.junit.Assert.assertTrue(data.nauticalDuskAngle > data.civilDuskAngle)
        org.junit.Assert.assertTrue(data.astroDuskAngle > data.nauticalDuskAngle)
        org.junit.Assert.assertTrue(data.nauticalDawnAngle > data.astroDawnAngle)
        org.junit.Assert.assertTrue(data.civilDawnAngle > data.nauticalDawnAngle)
        org.junit.Assert.assertTrue(data.sunriseAngle > data.civilDawnAngle)
        org.junit.Assert.assertTrue(data.solarNoonAngle >= 0f)
        org.junit.Assert.assertTrue(data.solarMidnightAngle >= 0f)
        org.junit.Assert.assertTrue(formatted.length >= 32)
    }

    @Test
    fun testDailyCacheAndEventProgression() {
        val lat = 48.1486
        val lon = 17.1077
        val date = LocalDate.of(2026, 6, 21) // Summer solstice

        // First calculation: runs astronomical sequence
        val data1 = SolarCalculator.calculateSolarData(lat, lon, date, currentTimeMillis = 0L)
        // Check event times
        val sunsetTime = data1.sunsetEpoch
        val civilDuskTime = data1.civilDuskEpoch
        org.junit.Assert.assertTrue(sunsetTime > 0L)
        org.junit.Assert.assertTrue(civilDuskTime > sunsetTime)

        // Time 1: 10 minutes before sunset
        val beforeSunsetMs = sunsetTime - 10 * 60 * 1000L
        val dataBeforeSunset = SolarCalculator.calculateSolarData(lat, lon, date, beforeSunsetMs)
        org.junit.Assert.assertEquals("Sunset", dataBeforeSunset.nextEventName)
        org.junit.Assert.assertEquals(sunsetTime, dataBeforeSunset.nextEventEpoch)

        // Time 2: 10 minutes after sunset (before civil dusk)
        val afterSunsetMs = sunsetTime + 10 * 60 * 1000L
        val dataAfterSunset = SolarCalculator.calculateSolarData(lat, lon, date, afterSunsetMs)
        org.junit.Assert.assertEquals("Civil Dusk", dataAfterSunset.nextEventName)
        org.junit.Assert.assertEquals(civilDuskTime, dataAfterSunset.nextEventEpoch)

        // Verify base astronomical values (sunrise, sunset, angles) remained identical
        org.junit.Assert.assertEquals(dataBeforeSunset.sunsetEpoch, dataAfterSunset.sunsetEpoch)
        org.junit.Assert.assertEquals(dataBeforeSunset.sunsetAngle, dataAfterSunset.sunsetAngle, 0.001f)
    }

    @Test
    fun testWisconsinNightUpcomingEvents() {
        val lat = 43.0731
        val lon = -89.4012
        val zone = java.time.ZoneId.of("America/Chicago")
        val date = LocalDate.of(2026, 10, 9)

        // 21:00 (9:00 PM) Central Time on Oct 9, 2026 - pitch black outside
        val ninePmMillis = date.atTime(21, 0).atZone(zone).toInstant().toEpochMilli()
        val dataAt9pm = SolarCalculator.calculateSolarData(lat, lon, date, ninePmMillis, zone)

        // Must NOT show Sunrise! Next upcoming event is Solar Midnight (~00:57 AM)
        org.junit.Assert.assertEquals("Solar Midnight", dataAt9pm.nextEventName)
        org.junit.Assert.assertTrue(dataAt9pm.nextEventEpoch > ninePmMillis)

        // 02:00 AM Central Time on Oct 10, 2026 - after solar midnight, before dawn
        val dateNext = LocalDate.of(2026, 10, 10)
        val twoAmMillis = dateNext.atTime(2, 0).atZone(zone).toInstant().toEpochMilli()
        val dataAt2am = SolarCalculator.calculateSolarData(lat, lon, dateNext, twoAmMillis, zone)

        // Next upcoming event must be Astro Dawn (~05:30 AM), NOT Sunrise
        org.junit.Assert.assertEquals("Astro Dawn", dataAt2am.nextEventName)
        org.junit.Assert.assertTrue(dataAt2am.nextEventEpoch > twoAmMillis)
    }
}

