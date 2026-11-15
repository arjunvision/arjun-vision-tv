# Architecture

Kotlin, Jetpack Compose (plain Compose + Material 3, no `tv-material` alpha), coroutines + StateFlow, DataStore, WorkManager. Single activity, single module, packages split by layer. Manual dependency wiring (no DI framework).

```
app/src/main/java/com/arjunvision/tvlauncher/
├── MainActivity.kt              HOME + LEANBACK_LAUNCHER activity, key dispatch, screensaver wake
├── domain/                      pure Kotlin (unit-tested on the JVM)
│   ├── models/                  Profile, AppSettings, LauncherApp, HdmiInput, ThemeSpec, InspirationItem, Festival
│   ├── repositories/            SettingsRepository interface
│   └── themes/                  ColorMath, ThemeFactory (15 designs), FestivalCalendar, ThemeScheduler
├── data/
│   ├── preferences/             DataStore settings (profile, inputs, Lite Mode, dock order, hidden apps...)
│   ├── applications/            PackageManager discovery (LEANBACK_LAUNCHER + LAUNCHER) and launch
│   └── themes/                  ThemeRepository (JSON), InspirationSources (Wikimedia, Pexels, Pixabay),
│                                ThemeAssetCache (download, resize, dedupe, clean-up), ThemeEngine
├── platform/
│   ├── hdmi/                    TvInputManager discovery + state, passthrough-URI switching
│   ├── network/                 NetworkMonitor: Wi-Fi vs VALIDATED internet, NetworkCallback flow
│   └── work/                    ThemeWorker (daily) + network-constrained discovery retry
└── ui/
    ├── LauncherViewModel.kt     navigation, settings, themes, screensaver idle timer
    ├── LauncherRoot.kt          live theming, screen transitions, Back rules
    ├── theme/                   design tokens (LauncherColors, LauncherStyle)
    ├── components/              FocusCard, GlassDock, AmbientLayers (aurora, particles, wallpaper)
    ├── startup/ home/ profiles/ apps/ gaming/ settings/ themes/ screensaver/
```

## Data flow

```
DataStore ──┐
PackageManager ─┤→ LauncherViewModel.uiState (StateFlow) → Compose screens
TvInputManager ─┘
ThemeRepository.state ──→ LauncherRoot → LauncherStyle.apply(theme) → every screen repaints
ThemeWorker (daily) → ThemeEngine.run() → discovery (if due + online) → apply scheduled theme → clean-up
```

## Theme engine

1. **Daily** (`ThemeWork.scheduleDaily`, WorkManager, no network constraint): apply today's theme from saved data, so festival switches happen offline too.
2. **Monthly discovery** (due after 25 days, or when today or a festival in the next 35 days has no generated theme):
   - Checks `NetworkState.internet` (VALIDATED, not just Wi-Fi), then the image service itself.
   - Offline or service down: keep the current theme, record the reason, and queue a `NetworkType.CONNECTED` job with exponential backoff (runs by itself when internet returns).
   - Online: at most 3 categories × 6 results per source, through official APIs over HTTPS only.
3. **Licence filter**: Wikimedia keeps only CC0, public domain, CC BY, CC BY-SA (NC refused). Pexels and Pixabay use their own licences. Each record stores title, image URL, page URL, creator, source, licence, licence URL and download permission.
4. **Assets**: one file per URL hash (no duplicates), resized to the panel width read from the display mode (1920 or 3840), JPEG 88, 25 MB download cap, broken URLs marked failed. Clean-up keeps saved themes, the active theme, the 2 newest per category and the 24 newest inspirations.
5. **Original theme**: `ThemeFactory.generate` = category base design + photo tint + corner variant. The primary accent always stays the category's own.
6. **Scheduler**: festival window > season > Classic. Manual Apply sets `locked = true`; the scheduler never replaces a locked theme. 7-day preview via `ThemeScheduler.upcoming`.

## Launcher registration

- `MAIN + HOME + DEFAULT` filter (selectable as Home in Android settings) and `MAIN + LEANBACK_LAUNCHER` (shows in the TV's Apps row).
- `singleTask`; the Home key → `onNewIntent` → back to Home screen.
- The original Xiaomi launcher is never disabled. Settings → "Default home app" opens Android's own chooser. On the MiTV-MOOR2 firmware the choice may not hold, because `setupwraith` also answers HOME (see DESIGN_RESEARCH.md).

## Permissions

`ACCESS_NETWORK_STATE`, `INTERNET` only. Cleartext traffic is off. No storage, location, accounts or analytics. API keys (optional) come from `local.properties` into `BuildConfig`. They are not in git, but anyone holding the APK can extract them, so use free-tier keys only.
