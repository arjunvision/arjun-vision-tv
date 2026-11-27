package com.arjunvision.tvlauncher.platform.home

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.arjunvision.tvlauncher.MainActivity

/**
 * "Arjun Vision Home button": when Google TV's home screen comes to the front (Home button,
 * power-on, leaving an app), bring ARJUN VISION TV forward instead.
 *
 * Why an accessibility service: on this Xiaomi firmware Android ignores HOME priority for
 * third-party apps, and disabling Google TV home breaks the remote's Settings button (tested,
 * reverted). Google TV stays installed and enabled; only its home screen is skipped.
 *
 * Privacy: the service config limits events to two packages (launcherx, setupwraith) and one event
 * type (window state changed). It reads only the package and class name of that screen. It cannot
 * read window content (canRetrieveWindowContent=false), filters no keys, and stores or sends nothing.
 * Sri switches it on himself in Settings > System > Accessibility; turning it off restores Google TV home.
 */
class HomeButtonService : AccessibilityService() {

    private var lastRedirect = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!StockHome.isStockHome(event.packageName?.toString(), event.className?.toString())) return
        val now = SystemClock.elapsedRealtime()
        if (StockHome.isPaused(now)) return
        if (now - lastRedirect < 800) return // never loop if something keeps re-showing Google TV home
        lastRedirect = now
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(StockHome.EXTRA_FROM_HOME_BUTTON, true),
            )
        }
    }

    override fun onInterrupt() = Unit
}

/** Pure rules for the Home button helper (unit-tested in StockHomeTest). */
object StockHome {
    const val EXTRA_FROM_HOME_BUTTON = "com.arjunvision.tvlauncher.FROM_HOME_BUTTON"

    const val LAUNCHERX = "com.google.android.apps.tv.launcherx"
    private const val SETUPWRAITH = "com.google.android.tungsten.setupwraith"

    /** The HOME screens found on Sri's MiTV-MOOR2 (adb query-activities, 27 Nov 2026). */
    private val homeScreens = setOf(
        "$LAUNCHERX.home.HomeActivity",
        "$SETUPWRAITH.RecoveryActivity",
    )

    /** Only the home screen itself. The Settings dashboard, search and other Google TV screens pass through. */
    fun isStockHome(packageName: String?, className: String?): Boolean {
        if (packageName != LAUNCHERX && packageName != SETUPWRAITH) return false
        return className in homeScreens
    }

    @Volatile private var pausedUntil = 0L

    /** "Open Google TV home" in Arjun Settings: let Google TV home stay for [millis]. */
    fun pause(now: Long, millis: Long = 60_000L) {
        pausedUntil = now + millis
    }

    fun isPaused(now: Long): Boolean = now < pausedUntil

    /** True when Sri has switched the helper on in Settings > Accessibility. */
    fun isEnabled(context: Context): Boolean {
        val me = ComponentName(context, HomeButtonService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.split(':').any { it.equals(me, ignoreCase = true) }
    }

    fun googleTvHomeIntent(): Intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setComponent(ComponentName(LAUNCHERX, "$LAUNCHERX.home.HomeActivity"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
