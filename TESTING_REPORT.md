# Testing report

Device: MiTV-MOOR2 (`venom`), Android 11 / API 30, build 2674, 1920×1080. Tests are driven over network ADB with the remote's key codes (`input keyevent`) and checked with `uiautomator dump`, `screencap`, `logcat`, `dumpsys`.
Marks: **PASS** = run and seen to work · **FAIL** · **NOT VERIFIED** = not run on the real TV.

## Phase 3 – Home + design system, Airtel direct HDMI (2026-11-15, release build)
| Check | Result | Evidence |
|---|---|---|
| AIRTEL DTH tile: one OK switches straight to the box's HDMI, no Settings | PASS | logcat: `START … passthrough/…/HW5 cmp=com.mitv.livetv` from our uid, `LiveTv_TuneManager tuneToInput … HW5` |
| Airtel port auto-detected and saved, reused after restart | PASS | Detected HW5 = HDMI 2 (only connected non-PS5 port); Settings shows "Airtel DTH → HDMI 2" after app restart |
| PS5 auto-mapped from its HDMI-CEC name | PASS | Settings shows "PlayStation 5 → PlayStation 5" (input HDMI100004) |
| Airtel picture actually visible on screen | PASS (Sri, by eye) | Sri confirmed the Airtel picture appears after OK on AIRTEL DTH (TV blocks screenshots of HDMI video) |
| Intent resolve check (must be a TV input app) | PASS | Resolves to `com.mitv.livetv`; other apps refused |
| One-time HDMI picker when detection is unsure | NOT VERIFIED on TV | Not triggered on this TV (detection was certain); logic unit-tested (`HdmiDetectionTest`, 4/4) |
| No false success message | PASS | No toast on switch; only failure shows manual remote steps |
| Tile text "AIRTEL DTH / Father's TV · Watch Live TV" | PASS | uiautomator text |
| My Mode order PS5, YouTube, Netflix, Hotstar, Gaming Centre, Entertainment, Theme Studio, Airtel, Apps, Settings | PASS | uiautomator + screenshot |
| Father Mode: Airtel first and wider, YouTube, Netflix, Hotstar, Settings; calmer (no particles, still aurora) | PASS | screenshot |
| Gaming Centre → Entertainment Centre with Gaming focused | PASS | screenshot |
| "Not installed" tile state | NOT VERIFIED on TV | All three apps are installed on this TV |
| OK debounce (double press launches once) | PASS | 1 START from our uid for a double OK on YouTube |
| Stray OK release ignored | Code fix | One unexplained Airtel switch was seen once during a key test and did not reproduce; tiles now act only if they saw the OK press |
| Back on Home stays in ARJUN VISION | PASS | Back on Home → still `com.arjunvision.tvlauncher` (before: dropped to Google TV home) |
| App list refreshes on install/remove | NOT VERIFIED on TV | Receiver for PACKAGE_ADDED/REMOVED/CHANGED registered; no app was installed during the test |
| Home D-pad frame time | IMPROVED, not 60 fps | 57 ms → 32 ms median (Lite off). atrace: GPU 45-53 → 17.6 ms per frame after baking wallpaper+scrim+aurora into one opaque layer and removing window/surface fills |
| Unit tests | PASS | 12/12 (HdmiDetection 4, ThemeScheduler 7, WeatherExclusion 1) |

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
