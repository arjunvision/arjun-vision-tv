package com.arjunvision.tvlauncher

import com.arjunvision.tvlauncher.domain.models.AppSettings
import com.arjunvision.tvlauncher.domain.models.ScreensaverStyle
import com.arjunvision.tvlauncher.ui.screensaver.ScreensaverMath
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreensaverMathTest {

    @Test
    fun slideshowWrapsAroundAndSurvivesAnEmptyList() {
        assertEquals(1, ScreensaverMath.nextIndex(0, 3))
        assertEquals(0, ScreensaverMath.nextIndex(2, 3))
        assertEquals(0, ScreensaverMath.nextIndex(5, 0))
    }

    @Test
    fun clockAlwaysStaysInsideTheOverscanSafeArea() {
        val random = Random(42)
        repeat(1_000) {
            val (x, y) = ScreensaverMath.nextSpot(random)
            assertTrue(x in 0.06f..0.94f)
            assertTrue(y in 0.08f..0.92f)
        }
    }

    @Test
    fun defaultsAreSlideshowWithLogo() {
        assertEquals(ScreensaverStyle.SLIDESHOW, AppSettings().screensaverStyle)
        assertTrue(AppSettings().screensaverLogo)
    }
}
