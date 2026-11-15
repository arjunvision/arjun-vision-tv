# CLAUDE.md – ARJUN VISION TV

Personal Android TV launcher for Sri's Xiaomi TV. Kotlin + Jetpack Compose, single `:app` module. The full brief lives in the conversation that started v2; the plan is in DEVELOPMENT_LOG.md.

## Commands
```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
~/Library/Android/sdk/platform-tools/adb connect 192.168.29.64:5555
~/Library/Android/sdk/platform-tools/adb -s 192.168.29.64:5555 install -r app/build/outputs/apk/release/app-release.apk
~/Library/Android/sdk/platform-tools/adb -s 192.168.29.64:5555 shell am start -n com.arjunvision.tvlauncher/.MainActivity
```
Release = R8, signed with this Mac's debug key (sideload only). `adb` is not on PATH; use the full path.

## Verified device facts (adb, 2026-11-15)
- MiTV-MOOR2 (device `venom`), Android 11 / API 30, build 2674, 1920×1080 @ 320 dpi (960×540 dp).
- Default HOME: `com.google.android.apps.tv.launcherx` (priority 2) + `setupwraith` (priority 1). Third-party launchers are capped at 0, so ours can't become Home without disabling them, and disabling them is forbidden (it also breaks the remote's Settings key; tested and undone).
- Live TV / input switching: `com.mitv.livetv` accepts `TvContract.buildChannelUriForPassthroughInput(id)` (verified for PS5). No `com.xiaomi.mitv.tvplayer`.
- HDMI: 4 CEC ports. PS5 = CEC 0x04, port 1, TvInput `com.mediatek.tvinput/.hdmi.HDMIInputService/HDMI100004`. Ports 1-3 = `.../HW4`, `HW5`, `HW6`. Airtel box has no CEC name.
- Speech service: `com.google.android.katniss` RecognitionService present (use NOT VERIFIED).
- Network ADB is not persistent (standby or reboot can drop it). Never change ADB settings; ask Sri to reconnect.

## Never do
- Root, firmware changes, debloating; disable, uninstall or hide Xiaomi or Google system packages.
- Change ADB, security or system settings without approval.
- Commit secrets (keys only in `local.properties` → BuildConfig).
- Record the microphone continuously; upload audio without consent.
- Hidden APIs, reflection into system services, undocumented HDMI commands.
- Delete user data or settings. Add Weather in any form (guarded by `WeatherExclusionTest`).
- Add a major library, a permission, an accessibility service, or change the architecture without asking.
- Commit before Sri confirms the phase on the TV. No force-push, no history rewrite, no branch deletion.

## Git
Branch `arjun-vision-v2`; baseline tag `pre-v2-baseline` (= `main`). One commit per phase after Sri's OK.
Rollback APK: `~/Desktop/ArjunVision-backup/arjunvision-0.2.0-vc2-installed.apk`.

## Phase status
| Phase | Status |
|---|---|
| 1 Device check + inspection + plan | Done, plan approved |
| 2 Backup, baseline tag, Weather proof | Done, waiting for Sri's check |
| 3 Home + design system | Next |
| 4 Smart HDMI Hub | – |
| 5 Cinematic Spotlight + motion | – |
| 6 Entertainment Centre | – (needs SOURCES.md + image loader approval) |
| 7 Voice Assistant | – (needs RECORD_AUDIO approval) |
| 8 Ambient Screensaver + DreamService | – |
| 9 Profiles, Settings, performance | – |
| 10 Acceptance tests | – |
