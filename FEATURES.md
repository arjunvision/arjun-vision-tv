# Features and test status (v0.2.0, 15 Nov 2026)

Device: **MiTV-MOOR2**, Android 11, API 30, security patch 2026-02-01 (read via `adb shell getprop`), 1920×1080 output.
"Tested on TV" = driven with `adb shell input keyevent` (the remote's key codes), checked with `uiautomator` text dumps, `screencap` screenshots and `logcat`.

| Feature | Status | Evidence |
|---|---|---|
| ADB over network (port 5555) | Tested on TV | `adb devices` → `device`, model MiTV_MOOR2 |
| Startup animation (logo, blue glow, tagline, ~2 s) | Tested on TV | Screenshot |
| Home dashboard (header, top shelf, glass dock) | Tested on TV | Screenshots |
| Father Mode (Cable TV first, large tiles, no PS5/Apps) | Tested on TV | Text dump |
| My Mode (PS5 first, apps, Tamil Cinema / Gaming / Tech News, Apps, Theme Studio) | Tested on TV | Text dump + screenshot |
| Profile switching + remembered (DataStore) | Tested on TV | Survived app restarts |
| D-pad navigation, OK, Back | Tested on TV | Right×N + OK opened Settings and Theme Studio; Back returned Home |
| Floating glass highlight + app-colour ambient glow | Tested on TV | Screenshots (pink rim on Theme Studio, green on Christmas preview) |
| App discovery (22 apps), YouTube, Netflix, JioHotstar tiles | Tested on TV | Package IDs confirmed with `pm list packages` |
| Launch YouTube search (Tamil Cinema tile) | Tested on TV | YouTube TV opened "Search results for Tamil cinema" |
| Launch Netflix / Hotstar from tiles | Built, not opened in this session | Same launch code as YouTube |
| HDMI discovery + connection state | Tested on TV | TV reports HDMI 1/2/3 + "PlayStation 5" (CEC); PS5 shows "Standby" |
| HDMI switching (PS5) via documented passthrough URI | Tested on TV | Live TV app `com.mitv.livetv` tuned to `HDMI100004` |
| Cable TV switching | Same code path, not run | Cable input not yet chosen (which port is the set-top box is your choice) |
| Network status: Wi-Fi vs real internet | Tested on TV (online) | "Online · Wi-Fi"; offline states not tested (turning off the TV's Wi-Fi would drop the adb link) |
| Online discovery (Wikimedia Commons, licence filter) | Tested on TV | 8 inspirations, all CC BY-SA, creator + licence stored |
| Original theme generation + caching (no duplicates) | Tested on TV | Monsoon + Christmas themes, 2 cached files (1 MB) |
| Automatic schedule (today = Monsoon season) | Tested on TV | Worker auto-applied Monsoon on first run |
| Preview → Apply → Lock; Unlock returns to schedule | Tested on TV | `state.json` before/after |
| Festival transitions, 7-day preview | Unit-tested (7 tests pass) | `ThemeSchedulerTest` |
| Offline retry when internet returns | Built, not tested on TV | WorkManager CONNECTED constraint + backoff |
| Pexels / Pixabay sources | Built, inactive | Need free API keys in `local.properties` |
| App library: hide/restore, add to dock, reorder | Tested on TV (MENU) | MENU opened Add to dock / Hide / Close. Hold-OK uses the same menu but was not driven (adb cannot reproduce an IR hold reliably) |
| Clock screensaver (drift every 60 s) | Tested on TV | Appeared after 2 min idle (test setting), any key returned Home. First attempt did not trigger, most likely because delayed key presses reset the idle timer; retest passed. Reset to 5 min |
| Lite Mode, card size, animation intensity, clock format | Built | Settings rows verified present |
| Memory | Measured | Release: 77 MB PSS (debug was 122-163 MB) |
| Cold start | Measured | Release: 0.86-0.90 s to first frame (debug: 5.4 s) |
| Default launcher | Not changed | Xiaomi home stays default; see ROADMAP |
| Release APK (R8, 2.3 MB, signed with this Mac's debug key for sideloading) | Installed on TV | `app/build/outputs/apk/release/app-release.apk` |
| Frame rate on Home (20 s idle, rain particles) | Measured, release | median 6 ms, 90th 17 ms, 12 % janky (first design: 61 ms median, 74 % janky; fixed by a stepped, tiny-bitmap aurora) |
