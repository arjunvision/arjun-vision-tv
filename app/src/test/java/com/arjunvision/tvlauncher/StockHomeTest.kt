package com.arjunvision.tvlauncher

import com.arjunvision.tvlauncher.platform.home.StockHome
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StockHomeTest {

    @Test
    fun googleTvHomeScreenIsRedirected() {
        assertTrue(StockHome.isStockHome("com.google.android.apps.tv.launcherx", "com.google.android.apps.tv.launcherx.home.HomeActivity"))
        assertTrue(StockHome.isStockHome("com.google.android.tungsten.setupwraith", "com.google.android.tungsten.setupwraith.RecoveryActivity"))
    }

    @Test
    fun otherGoogleTvScreensPassThrough() {
        // The remote's Settings button opens this dashboard; it must keep working.
        assertFalse(StockHome.isStockHome("com.google.android.apps.tv.launcherx", "com.google.android.apps.tv.launcherx.dashboard.DashboardActivity"))
        assertFalse(StockHome.isStockHome("com.google.android.apps.tv.launcherx", "android.app.Dialog"))
        assertFalse(StockHome.isStockHome("com.google.android.apps.tv.launcherx", null))
        assertFalse(StockHome.isStockHome("com.netflix.ninja", "com.google.android.apps.tv.launcherx.home.HomeActivity"))
        assertFalse(StockHome.isStockHome(null, null))
    }

    @Test
    fun pauseLetsGoogleTvHomeStayForAMinute() {
        StockHome.pause(now = 1_000L)
        assertTrue(StockHome.isPaused(30_000L))
        assertFalse(StockHome.isPaused(61_001L))
    }
}
