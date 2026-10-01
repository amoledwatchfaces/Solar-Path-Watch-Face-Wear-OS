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
        val formatted = SolarCalculator.formatAnglesForComplication(data, "SUNSET")
        println("Formatted string: $formatted")

        org.junit.Assert.assertTrue(data.civilDuskAngle > data.sunsetAngle)
        org.junit.Assert.assertTrue(data.nauticalDuskAngle > data.civilDuskAngle)
        org.junit.Assert.assertTrue(data.astroDuskAngle > data.nauticalDuskAngle)
        org.junit.Assert.assertTrue(data.nauticalDawnAngle > data.astroDawnAngle)
        org.junit.Assert.assertTrue(data.civilDawnAngle > data.nauticalDawnAngle)
        org.junit.Assert.assertTrue(data.sunriseAngle > data.civilDawnAngle)
        org.junit.Assert.assertTrue(formatted.length >= 26)
    }
}
