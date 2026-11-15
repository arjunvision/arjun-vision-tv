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
