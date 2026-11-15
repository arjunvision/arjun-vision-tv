package com.arjunvision.tvlauncher

import com.arjunvision.tvlauncher.domain.models.ThemeCategory
import com.arjunvision.tvlauncher.domain.themes.ColorMath
import com.arjunvision.tvlauncher.domain.themes.ThemeFactory
import com.arjunvision.tvlauncher.domain.themes.ThemeScheduler
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeSchedulerTest {

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Test fun pongalLaunchWeekUsesPongalTheme() {
        assertEquals(ThemeCategory.PONGAL, ThemeScheduler.decide(d(2027, 1, 13)).category) // Bhogi
        assertEquals(ThemeCategory.PONGAL, ThemeScheduler.decide(d(2027, 1, 14)).category)
        assertEquals(ThemeCategory.PONGAL, ThemeScheduler.decide(d(2027, 1, 16)).category) // Kaanum Pongal
        assertEquals(ThemeCategory.CLASSIC, ThemeScheduler.decide(d(2027, 1, 18)).category)
    }

    @Test fun sevenDayPreviewBeforePongal() {
        val preview = ThemeScheduler.upcoming(d(2027, 1, 6))
        assertNotNull(preview)
        assertEquals(ThemeCategory.PONGAL, preview!!.festival.category)
        assertEquals(7L, preview.daysAway)
        assertNull(ThemeScheduler.upcoming(d(2026, 12, 1))) // nothing within 7 days
    }

    @Test fun festivalTransitionsAcrossTheYearEnd() {
        assertEquals(ThemeCategory.CHRISTMAS, ThemeScheduler.decide(d(2026, 12, 20)).category)
        assertEquals(ThemeCategory.CHRISTMAS, ThemeScheduler.decide(d(2026, 12, 26)).category)
        assertEquals(ThemeCategory.CLASSIC, ThemeScheduler.decide(d(2026, 12, 28)).category)
        assertEquals(ThemeCategory.NEW_YEAR, ThemeScheduler.decide(d(2026, 12, 31)).category)
        assertEquals(ThemeCategory.NEW_YEAR, ThemeScheduler.decide(d(2027, 1, 2)).category)
    }

    @Test fun seasonsAndFixedFestivals() {
        assertEquals(ThemeCategory.MONSOON, ThemeScheduler.decide(d(2026, 11, 15)).category)
        assertEquals(ThemeCategory.SUMMER, ThemeScheduler.decide(d(2027, 5, 10)).category)
        assertEquals(ThemeCategory.TAMIL_NEW_YEAR, ThemeScheduler.decide(d(2027, 4, 14)).category) // beats Summer
        assertEquals(ThemeCategory.INDEPENDENCE_DAY, ThemeScheduler.decide(d(2027, 8, 15)).category)
        assertEquals(ThemeCategory.DIWALI, ThemeScheduler.decide(d(2027, 10, 29)).category)
        assertEquals(ThemeCategory.CLASSIC, ThemeScheduler.decide(d(2027, 3, 3)).category)
    }

    @Test fun preparesUpcomingFestivalsMonthAhead() {
        val list = ThemeScheduler.categoriesToPrepare(d(2026, 12, 10))
        assertEquals(ThemeCategory.MONSOON, list.first())
        assertTrue(ThemeCategory.CHRISTMAS in list)
        assertTrue(ThemeCategory.NEW_YEAR in list)
    }

    @Test fun generatedThemeKeepsBrandAccentButTakesPhotoTint() {
        val base = ThemeFactory.builtIn(ThemeCategory.DIWALI)
        val gen = ThemeFactory.generate(ThemeCategory.DIWALI, null, null, 0xFF00FF00, 1_800_000_000_000)
        assertEquals(base.accent, gen.accent)
        assertNotEquals(base.accent2, gen.accent2)
        assertTrue(gen.generated)
    }

    @Test fun dominantColourIgnoresGreysAndFindsVividHue() {
        val pixels = IntArray(100) { if (it < 70) 0xFF808080.toInt() else 0xFFE11D48.toInt() }
        val c = ColorMath.dominantVividColor(pixels)!!
        assertEquals(0xE1, ColorMath.red(c))
        assertNull(ColorMath.dominantVividColor(IntArray(10) { 0xFF777777.toInt() }))
    }
}
