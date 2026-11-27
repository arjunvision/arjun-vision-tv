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
 * Privacy: the service config limits events to two packages (launcherx, setupwraith) and two event
 * types (window state changed, view focused). It reads only the package and class name. It cannot
 * read window content (canRetrieveWindowContent=false), filters no keys, and stores or sends nothing.
 * Sri switches it on himself in Settings > System > Accessibility; turning it off restores Google TV home.
 *
 * Focus events close a gap: when Google TV home is ALREADY in front (after an app update reconnects
 * the helper, or after "Open Google TV home" ends), pressing Home or moving on it shows no new window,
 * only a focus change inside Google TV home; that now brings Arjun Vision forward too.
 */
class HomeButtonService : AccessibilityService() {

    private var lastRedirect = 0L
    /** Last full screen Google TV showed (HomeActivity, the Settings dashboard...); null = not known yet. */
    private var lastStockScreen: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val decision = StockHome.decide(event.eventType, event.packageName?.toString(), event.className?.toString(), lastStockScreen)
        lastStockScreen = decision.lastScreen
        if (!decision.redirect) return
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

    data class Decision(val redirect: Boolean, val lastScreen: String?)

    /**
     * Window change to Google TV home → redirect. Focus moving inside Google TV while its last full
     * screen was home (or unknown, e.g. right after an update) → redirect. Focus inside another Google TV
     * screen (the Settings dashboard from the remote's Settings key) → leave it alone.
     */
    fun decide(eventType: Int, packageName: String?, className: String?, lastScreen: String?): Decision {
        if (packageName != LAUNCHERX && packageName != SETUPWRAITH) return Decision(false, lastScreen)
        return when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val screen = className?.takeIf { it.startsWith(packageName) && it.endsWith("Activity") } ?: lastScreen
                Decision(isStockHome(packageName, className), screen)
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> Decision(lastScreen == null || lastScreen in homeScreens, lastScreen)
            else -> Decision(false, lastScreen)
        }
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
