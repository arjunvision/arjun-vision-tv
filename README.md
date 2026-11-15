# ARJUN VISION TV

**ARJUN VISION TECH SOLUTIONS · YOUR SMART TV EXPERIENCE**

A personal Android TV launcher (Kotlin + Jetpack Compose) for the Xiaomi **MiTV-MOOR2** (Android TV 11, API 30). FUTURE CINEMA OS design: Father Mode with Cable TV first, My Mode with PlayStation 5 first, a frosted glass dock with a springing glass highlight, app-colour ambient glow, and an Online Theme Studio that builds original festival and seasonal themes from licensed photos.

Docs: [FEATURES.md](FEATURES.md) (what works, what was tested) · [ARCHITECTURE.md](ARCHITECTURE.md) · [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) · [DESIGN_RESEARCH.md](DESIGN_RESEARCH.md) · [ROADMAP.md](ROADMAP.md)

## Requirements (this Mac)

- Android SDK at `~/Library/Android/sdk` (platform 36, build-tools 35/36, platform-tools)
- JDK 17 or newer (built here with Temurin 25)
- Gradle wrapper included (Gradle 9.2.1, AGP 8.13.1, Kotlin 2.2.21)
- TV and Mac on the same Wi-Fi; on the TV: Developer options → USB debugging on

## Connect to the TV

```bash
~/Library/Android/sdk/platform-tools/adb connect 192.168.29.64:5555
```

```bash
~/Library/Android/sdk/platform-tools/adb devices -l
```

First time only: the TV shows "Allow USB debugging?". Tick **Always allow from this computer** and press OK. `adb devices` must show `device` (not `unauthorized`).

## Build and install

```bash
cd "ArjunVisionTV" && ./gradlew :app:testDebugUnitTest :app:assembleDebug
```

```bash
~/Library/Android/sdk/platform-tools/adb -s 192.168.29.64:5555 install -r app/build/outputs/apk/debug/app-debug.apk
```

```bash
~/Library/Android/sdk/platform-tools/adb -s 192.168.29.64:5555 shell am start -n com.arjunvision.tvlauncher/.MainActivity
```

On the TV you can also open it from the Apps row: **ARJUN VISION TV**.

## First-time setup on the TV (remote only)

1. Settings → **Cable TV HDMI input**: press OK until it shows your set-top box input. The TV reports `HDMI 1`, `HDMI 2`, `HDMI 3` and `PlayStation 5` (the PS5's own HDMI-CEC name).
2. Settings → **PlayStation 5 HDMI input**: choose `PlayStation 5`.
3. Profile button in the header → Father Mode or My Mode. The choice is remembered.

## Remote

| Key | Action |
|---|---|
| D-pad | Move focus (glass highlight follows) |
| OK | Open |
| Hold OK (½ s) or MENU | App library tile menu: add to dock, move left/right, hide |
| Back | Returns to Home from every screen (preview → Theme Studio) |
| Home | TV's normal Home behaviour (unchanged) |
| Any key | Wakes the clock screensaver |

## Online Theme Studio

- Works with no setup: uses the **Wikimedia Commons** API (no key), freely licensed photos only, credit shown on screen.
- Optional extra sources: add free keys to `local.properties` (never committed) and rebuild:
  ```
  PEXELS_API_KEY=your_key
  PIXABAY_API_KEY=your_key
  ```
- Unsplash is not used: its API rules require hotlinking images, which conflicts with offline caching.
- Automatic: daily check; monthly discovery; festival themes switch on their dates; 7-day preview before a festival; Apply locks a theme until you unlock it.
- Offline: keeps the current theme and cached wallpapers, and retries discovery when internet returns.

## Rollback and launcher recovery

- **The TV's own home is untouched.** On this TV that's Google TV's launcher (`com.google.android.apps.tv.launcherx`), and it stays the default. ARJUN VISION TV opens from the Apps row; the Home key returns to Google TV home.
- **If Google TV home ever goes missing** (for example after an experiment), run `./tools/restore-google-tv-home.sh`. It re-enables `launcherx` and `setupwraith` and sets Google TV home back. This was tested on 2026-11-15.
- **Go back to the pre-v2 app:** install `~/Desktop/ArjunVision-backup/arjunvision-0.2.0-vc2-installed.apk`:
  ```bash
  ~/Library/Android/sdk/platform-tools/adb -s 192.168.29.64:5555 install -r -d ~/Desktop/ArjunVision-backup/arjunvision-0.2.0-vc2-installed.apk
  ```
- **Go back to the pre-v2 code:** `git checkout pre-v2-baseline` (read-only look) or `git switch main`.
- **Clear launcher defaults** (only if ARJUN VISION was ever chosen as Home): TV Settings → Apps → ARJUN VISION TV → Clear defaults. Or uninstall the app (below).

## Uninstall

```bash
~/Library/Android/sdk/platform-tools/adb -s 192.168.29.64:5555 uninstall com.arjunvision.tvlauncher
```

## Privacy

Permissions: network state + internet only. HTTPS only. No accounts, analytics, viewing history or uploads. Images are fetched only from the official APIs listed above.

## Assets

- Logo: `app/src/main/res/drawable-nodpi/arjun_vision_logo.png`, copied from `~/Claude-Creative-Studio/brand-assets/arjun-vision-bulb-logo.png` (your file). Replace it there with the official artwork if this is not the final version.
- Wallpapers: downloaded at runtime with their licence and credit stored per image. None ship in the APK.
- Reference repo (`../references/cleanyourtv`, MIT) was studied only; no code or assets were copied.
