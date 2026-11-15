# Development log

## 2026-11-15 – v0.1 to v0.2 (before the v2 brief)
- Built the launcher from scratch: startup, Father/My Mode, app discovery, HDMI switching (PS5 verified), Settings, DataStore.
- Online Theme Studio (Wikimedia Commons, licence filter, scheduler, 7 JVM tests), FUTURE CINEMA OS redesign (glass dock, aurora, ambient glow), clock screensaver, Lite Mode.
- Performance: Home frames went from 61 ms median / 74 % janky to 6 ms / 12 % (release) with a stepped, 160×90-bitmap aurora. Release cold start 0.9 s, 77 MB PSS.
- Default-Home attempt (Sri approved): disabled `launcherx` + `setupwraith`, Home key worked, but the remote's **Settings key broke** → fully reverted with `tools/restore-google-tv-home.sh`. Verified 0 disabled packages afterwards.

## Phase 1 – Device check + inspection (2026-11-15)
- Ran every check from the v2 brief over adb (model, device, SDK, build, size, density, HOME resolution, tv_input, packages, RecognitionService, hdmi_control, DreamServices).
- Inspection report and phased plan written and approved (plan file in `~/.claude/plans/squishy-bubbling-meadow.md`; summary in CLAUDE.md).
- Read-only study of pengchujin/aerial-tv (MIT) started for Phases 4 and 8.
- Decision: git repo stays in the Google Drive folder (Sri).

## Phase 2 – Safety + Weather proof (2026-11-15)
- `git init` (branch `main`), baseline commit `cf07f46`, tag `pre-v2-baseline`, working branch `arjun-vision-v2`. `.kotlin/` added to `.gitignore`. No secrets tracked (`local.properties` ignored).
- Backed up the APK installed on the TV (versionCode 2, release build, 2.3 MB) to `~/Desktop/ArjunVision-backup/` with `version.txt`.
- Weather: whole-project keyword search → 0 matches in app code; the only match is a description of another launcher in DESIGN_RESEARCH.md. Added `WeatherExclusionTest` (fails if any weather keyword appears in `src/main`). No location permission exists.
- Added CLAUDE.md, DEVELOPMENT_LOG.md, TESTING_REPORT.md, NOTICE. No app code changed, so no reinstall was needed.
- Changed files: `.gitignore`, `app/src/test/.../WeatherExclusionTest.kt`, `CLAUDE.md`, `DEVELOPMENT_LOG.md`, `TESTING_REPORT.md`, `NOTICE`, `README.md` (rollback section).

## Phase 3 – Home + design system + Airtel direct HDMI (2026-11-15)
- Tiles per v2: AIRTEL DTH (Father's TV · Watch Live TV), PLAYSTATION 5 (My Gaming), YouTube, Netflix, Disney+ Hotstar (always shown; "Not installed" opens Play Store), Gaming Centre, Entertainment Centre, Theme Studio, App library, Settings. Existing user-added dock apps kept.
- Entertainment Centre shell screen: the three YouTube search shortcuts from v0.2 moved here (no fabricated content; live feeds wait for SOURCES.md approval in Phase 6).
- Father Mode: Airtel first and 1.25× wide, tiles ≥ 270 dp, half zoom, still aurora, no particles.
- **Airtel direct switching (Sri's request)**: `HdmiDetection` picks the only connected HDMI port that is not the PS5's (HW5 = HDMI 2 on this TV), saves it, reuses it; one-time picker only if unsure. PS5 auto-mapped from its CEC child "PlayStation 5". Intent must resolve to a TV input app (`com.mitv.livetv`), launched explicitly. Hidden inputs (`TvInputInfo.isHidden`) respected. No success toasts.
- Remote: OK debounce (600 ms, shared), stray OK releases ignored, Back on Home consumed.
- Package add/remove receiver refreshes tiles. 5 % overscan margins (48 dp × 28 dp).
- Performance: atrace showed the Home screen GPU-bound (~6 full-screen fills per frame). New `CinematicBackdrop` bakes wallpaper + scrim + aurora into one opaque 960×540 image off the main thread; transparent theme Surface; window background dropped after first frame; glass highlight now moves as a cached layer. Frame median 57 → 32 ms; GPU 45-53 → 17.6 ms.
- Reference: pengchujin/aerial-tv (MIT) studied read-only: CEC-child-first switching, resolve check, hidden inputs. No code copied.
- Changed files: `domain/models/{Models,HdmiDetection}.kt`, `platform/hdmi/HdmiInputRepository.kt`, `ui/LauncherViewModel.kt`, `ui/LauncherRoot.kt`, `ui/home/{HomeScreen,SetTopBoxPicker}.kt`, `ui/entertainment/EntertainmentScreen.kt`, `ui/components/{FocusCard,GlassDock,CinematicBackdrop,AmbientLayers}.kt`, `ui/theme/Theme.kt`, `ui/settings/SettingsScreen.kt`, `ui/gaming/PlayStationScreen.kt`, `ui/themes/ThemeStudioScreen.kt`, `ui/screensaver/ScreensaverScreen.kt`, `MainActivity.kt`, tests `HdmiDetectionTest.kt`.
