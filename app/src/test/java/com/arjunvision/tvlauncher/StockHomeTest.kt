package com.arjunvision.tvlauncher

import com.arjunvision.tvlauncher.platform.home.StockHome
import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertEquals
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

    private val home = "com.google.android.apps.tv.launcherx.home.HomeActivity"
    private val dashboard = "com.google.android.apps.tv.launcherx.dashboard.DashboardActivity"
    private val gtv = "com.google.android.apps.tv.launcherx"

    @Test
    fun focusOnGoogleTvHomeRedirects() {
        // Already on Google TV home and the user presses Home / moves: only a focus event arrives.
        val d = StockHome.decide(AccessibilityEvent.TYPE_VIEW_FOCUSED, gtv, "android.widget.FrameLayout", home)
        assertTrue(d.redirect)
        // Right after an app update the helper does not know the screen yet: redirect too.
        assertTrue(StockHome.decide(AccessibilityEvent.TYPE_VIEW_FOCUSED, gtv, "android.view.View", null).redirect)
    }

    @Test
    fun settingsDashboardKeepsWorking() {
        val open = StockHome.decide(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, gtv, dashboard, home)
        assertFalse(open.redirect)
        assertEquals(dashboard, open.lastScreen)
        assertFalse(StockHome.decide(AccessibilityEvent.TYPE_VIEW_FOCUSED, gtv, "android.widget.Button", open.lastScreen).redirect)
    }

    @Test
    fun dialogsDoNotChangeTheKnownScreen() {
        val d = StockHome.decide(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, gtv, "android.app.Dialog", dashboard)
        assertFalse(d.redirect)
        assertEquals(dashboard, d.lastScreen)
    }

    @Test
    fun otherAppsAreIgnored() {
        assertFalse(StockHome.decide(AccessibilityEvent.TYPE_VIEW_FOCUSED, "com.netflix.ninja", "x", null).redirect)
    }

    @Test
    fun pauseLetsGoogleTvHomeStayForAMinute() {
        StockHome.pause(now = 1_000L)
        assertTrue(StockHome.isPaused(30_000L))
        assertFalse(StockHome.isPaused(61_001L))
    }
}
