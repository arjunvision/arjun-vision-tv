package com.arjunvision.tvlauncher

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Weather is excluded from ARJUN VISION TV (v2 brief, section 12). This guard fails the build's
 * unit tests if any weather feature, API, string or unit sneaks into the app source or manifest.
 */
class WeatherExclusionTest {

    private val pattern = Regex(
        "weather|forecast|temperature|humidity|openweather|open-meteo|accuweather|weatherapi|°C|°F",
        RegexOption.IGNORE_CASE,
    )

    @Test fun appSourceContainsNoWeatherCode() {
        val root = listOf(File("src/main"), File("app/src/main")).firstOrNull { it.isDirectory }
            ?: error("src/main not found from ${File(".").absolutePath}")
        val hits = root.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "xml", "json", "txt") }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { i, line ->
                    if (pattern.containsMatchIn(line)) "${file.path}:${i + 1}: ${line.trim()}" else null
                }
            }
            .toList()
        assertTrue("Weather references found:\n" + hits.joinToString("\n"), hits.isEmpty())
    }
}
