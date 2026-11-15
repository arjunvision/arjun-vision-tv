# Testing report

Device: MiTV-MOOR2 (`venom`), Android 11 / API 30, build 2674, 1920×1080. Tests are driven over network ADB with the remote's key codes (`input keyevent`) and checked with `uiautomator dump`, `screencap`, `logcat`, `dumpsys`.
Marks: **PASS** = run and seen to work · **FAIL** · **NOT VERIFIED** = not run on the real TV.

## Phase 2 – Safety + Weather proof (2026-11-15)
| Check | Result | Evidence |
|---|---|---|
| Git baseline + tag + branch | PASS | `cf07f46 (tag: pre-v2-baseline, main)`, branch `arjun-vision-v2` |
| No secrets in git | PASS | `git ls-files` has no local.properties / keystore / .env |
| Installed APK backed up | PASS | `~/Desktop/ArjunVision-backup/arjunvision-0.2.0-vc2-installed.apk` (2,334,950 bytes, sha256 1d751963…) + version.txt |
| Weather keyword search | PASS | `weather|forecast|temperature|humidity|openweather|open-meteo|accuweather|weatherapi|°C|°F`, whole project minus build outputs: 1 match, `DESIGN_RESEARCH.md:51` (describes the third-party "Lumen" launcher; not a feature) |
| Weather guard test | PASS | `WeatherExclusionTest` 1/1; all unit tests 8/8 |
| No location permission | PASS | Manifest: `ACCESS_NETWORK_STATE`, `INTERNET` only |
| Weather network requests (2 min logcat incl. screensaver) | NOT VERIFIED | Planned for Phase 10 (only Wikimedia calls exist in code) |

Notes: the **Monsoon** theme and **rain particles** are calendar-based visuals (mid-Oct to mid-Dec), not weather data.

## Earlier device results (v0.2.0, before v2) – kept for regression
| Check | Result |
|---|---|
| HDMI switch to PS5 via passthrough URI (`HDMI100004`) | PASS (Live TV `com.mitv.livetv` tuned to it) |
| Profile switch + persistence, D-pad/OK/Back | PASS |
| YouTube search tile | PASS |
| Online theme discovery + auto-apply + preview/apply/lock | PASS |
| Clock screensaver at timeout, any key exits | PASS (second attempt) |
| Release cold start | PASS, 0.86-0.90 s |
| Offline retry | NOT VERIFIED |
| Cable/Airtel switch | NOT VERIFIED (input not mapped yet) |
| Default Home takeover | FAIL by design: Settings key broke; reverted |
