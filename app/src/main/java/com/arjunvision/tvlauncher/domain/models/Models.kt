package com.arjunvision.tvlauncher.domain.models

/** Application-level profiles inside the launcher (not Android multi-user profiles). */
enum class Profile(val title: String, val subtitle: String) {
    FATHER("Father Mode", "Simple TV with Cable TV first"),
    MY("My Mode", "Gaming and digital entertainment"),
}

enum class CardSize(val label: String, val widthDp: Int) {
    COMPACT("Compact", 200),
    STANDARD("Standard", 230),
    LARGE("Large", 260),
}

/** [factor] scales focus-zoom and entrance motion. OFF disables motion-based effects. */
enum class AnimationLevel(val label: String, val factor: Float) {
    OFF("Off", 0f),
    SUBTLE("Subtle", 0.5f),
    NORMAL("Normal", 1f),
    FULL("Full", 1.5f),
}

data class AppSettings(
    val profile: Profile = Profile.FATHER,
    val clock24h: Boolean = false,
    val cardSize: CardSize = CardSize.STANDARD,
    val animation: AnimationLevel = AnimationLevel.NORMAL,
    val startupAnimation: Boolean = true,
    /** TvInputInfo.id of the HDMI input the cable box is on. Null until the user picks it. */
    val cableInputId: String? = null,
    /** TvInputInfo.id of the HDMI input the PS5 is on. Null until the user picks it. */
    val playStationInputId: String? = null,
    /** Lite Mode: no aurora motion or particles, for slower TVs or low memory. */
    val liteMode: Boolean = false,
    /** Idle minutes before the clock screensaver. 0 = off. */
    val screensaverMinutes: Int = 5,
    /** Apps hidden from the library (restorable). */
    val hiddenPackages: Set<String> = emptySet(),
    /** Dock favourites in order. Null = default (YouTube, Netflix, Hotstar when installed). */
    val favourites: List<String>? = null,
    /** Home wallpaper: null = automatic (Iron Man heroes if present), "theme" = theme photo, else a wallpaper id. */
    val wallpaperId: String? = null,
    /** Look of the screensaver (inside the launcher and Android's own screensaver). */
    val screensaverStyle: ScreensaverStyle = ScreensaverStyle.SLIDESHOW,
    /** Small bulb logo + "ARJUN VISION" under the screensaver clock. */
    val screensaverLogo: Boolean = true,
)

enum class ScreensaverStyle(val label: String) {
    SLIDESHOW("Cinematic slideshow"),
    HOME_WALLPAPER("Home wallpaper"),
    DARK("Dark clock"),
}

/** A launchable app discovered through PackageManager. */
data class LauncherApp(
    val packageName: String,
    val activityName: String,
    val label: String,
)

/** An HDMI input reported by the TV's TvInputManager. [portNumber] is null if the TV does not report it. */
data class HdmiInput(
    val inputId: String,
    val label: String,
    val portNumber: Int?,
    val connection: InputConnection = InputConnection.UNKNOWN,
    /** Set for an HDMI-CEC device input (e.g. "PlayStation 5"); points at its physical port input. */
    val parentId: String? = null,
    /** Name the user gave this input in the TV's own settings (TvInputInfo.loadCustomLabel), if any. */
    val customLabel: String? = null,
    /** "HDMI", "DisplayPort", "Other"… from TvInputInfo.getType(). */
    val typeLabel: String = "HDMI",
)

/** From TvInputManager.getInputState(). */
enum class InputConnection(val label: String) {
    CONNECTED("Connected"),
    STANDBY("Standby"),
    DISCONNECTED("Not connected"),
    UNKNOWN(""),
}

/** Entertainment apps with shortcut cards. Only shown when one of [packages] is actually installed. */
enum class KnownEntertainmentApp(val title: String, val packages: Set<String>) {
    YOUTUBE("YouTube", setOf("com.google.android.youtube.tv", "com.google.android.youtube")),
    NETFLIX("Netflix", setOf("com.netflix.ninja")),
    HOTSTAR("Disney+ Hotstar", setOf("in.startv.hotstar")),
    ;

    /** The Android TV package to open in the Play Store when the app is not installed (verified on the TV). */
    val storePackage: String get() = packages.first()
}
