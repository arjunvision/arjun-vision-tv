# Design research: CleanYourTV and showcase launchers

Reference clone: `../references/cleanyourtv` (kept outside this project, read only).
Studied: README, PROMPT, CLAUDE/AGENTS, SHOWCASE, LICENSE, `launcher/` source, `themes/`, `tools/`, `examples/`, `docs/`, `tests/`.
The companion phone app (udhaykumarbala/cleanyourtv-app, MIT) was studied from its README and v0.4.8 release notes.

## Licences

| Item | Licence | What we did |
|---|---|---|
| CleanYourTV code | MIT | Studied only. No code copied; ARJUN VISION TV is written from scratch in Kotlin + Compose. |
| Outfit font in their launcher | SIL OFL 1.1 | Not used. |
| Fluent emoji icons in their launcher | own licence file | Not used. |
| Theme wallpapers (aurora, glass, diwali, ambient-glass, ...) | mixed: CC0, CC BY, CC BY-SA, Unsplash; aurora and glass have no credits file | **Not used.** Our wallpapers come from our own licence-filtered discovery (see below). |
| Companion app | MIT | Not used; it is a phone remote for debloating, outside our scope. |

## What CleanYourTV is

1. A debloat procedure (`pm disable-user` of ad/telemetry packages, with backup and restore).
2. A tiny Java/Views launcher (about 572 KB, minSdk 26, no network permission) with an Apple TV-style dock, a floating glass highlight, ambient glow, themes pushed over adb, an inputs menu and a clock screensaver.

The companion app runs the same job from a phone. It is useful for non-technical owners, but it changes system packages, which we must not do without your approval.

## Technical findings that we adopted (re-implemented, not copied)

| Idea | How CleanYourTV does it | ARJUN VISION TV |
|---|---|---|
| Floating glass highlight | One overlay View, two SpringAnimations (stiffness ~260, damping 0.58), gradient fill + sheen + rim + 12 glow strokes, hardware layer | `GlassDock`: one Compose overlay, `Animatable<Rect>` spring (300 / 0.62), gradient body, rim in the app colour, 5 glow strokes. No blur. |
| App-colour ambient glow | Own vivid-colour code on a 12×12 / 24×24 icon bitmap; a 480×270 tinted mask stretched full screen | `ColorMath.dominantVividColor` on a 24×24 icon; `AuroraBackground` radial fields tinted with the focused app colour, 360 ms colour animation |
| Blur without RenderEffect | None anywhere; tiny bitmaps + bilinear upscale | Same rule: no `RenderEffect` (API 31+, our TV is API 30). Frosted glass = white-alpha gradients |
| Lite / reduced motion | `reduced_motion`, `animations=false` | Settings → Lite Mode: no aurora motion, no particles, half zoom, instant highlight |
| Long-press OK on IR remotes | Times the hold from `downTime` (550 ms); MENU opens the same menu | `FocusCard`: same timing approach; MENU key too |
| Wallpaper memory | Cap 1920 px, RGB_565, off the main thread | `decodeSampled`: same |
| Inputs | `TvInputManager`, passthrough only, `getInputState`, CEC child rows, `buildChannelUriForPassthroughInput` | Same documented APIs. **Verified on your TV**: the Live TV app switched to "PlayStation 5" (HDMI100004) |
| Clock screensaver | Fade out, jump to a random spot every 60 s | `ScreensaverScreen`: same burn-in protection, plus theme particles |
| Themes without rebuild | `config.json` pushed with adb into a content provider | Online Theme Studio: themes generated and stored on the TV (`files/themes/state.json`), no adb needed |

## Your exact TV (from `examples/xiaomi-mitv-moor2`)

- Live TV app `com.mitv.livetv` owns input selection. Never disable it.
- Google's `setupwraith` also answers HOME on this firmware, and `cmd package set-home-activity` "succeeds" without effect. Making a third-party launcher the true Home needs disabling `setupwraith` or the stock launcher, or an accessibility helper. **We did none of this** (your rule: no debloating without approval).
- With the stock launcher disabled, the remote's Settings button stopped working in their test.
- HDMI was untested in their report; we tested it (works).

## Showcase projects (from SHOWCASE.md, not run by us)

| Project | Idea worth noting | Stated device | Production-ready? |
|---|---|---|---|
| Home Deck | Neon glowing tiles, CEC-named HDMI, Lite mode, drifting screensaver | Sharp ATV 11 | Community project, unverified |
| KIDŌ | Command-centre home, seven-segment clock, input alert | Sharp ATV 11, Mi TV P1 | unverified |
| Lumen | Time-of-day / weather scene, frosted cards lit in app colour | TCL GTV 11 | unverified |
| SamuraiOS | Cyberpunk HUD with CPU/RAM | Android 9+ | unverified |
| Minimal Glass | tvOS dock, folders, palettes, offline | TCL ATV 12 | unverified |
| SwiftOS v3 | Liquid Glass dock with app-colour glow, continuous springs | Xiaomi API 28 | unverified |
| CleanXiaomiTV | Liquid-glass home over photo of the day | **Xiaomi MiTV-MOOR2**, your model | unverified on our unit |
| XMB Home | PS3-style cross bar with wave sheets, no permissions | Philips GTV 14 | unverified |

None of these were installed or tested here. A success on another Xiaomi model is not proof for the MiTV-MOOR2.

## Design direction comparison

| Direction | Strength | Risk on Mi TV (MediaTek, API 30) | Decision |
|---|---|---|---|
| Apple TV cinematic dashboard | Calm, big type, top shelf | Low | **Base layout** (top shelf + dock) |
| Aurora atmospheric | Premium depth with no images | Full-screen animated gradients can drop frames on weak GPUs | **Use**, slow 24 s cycle, off in Lite Mode |
| Liquid Glass | Modern | Real blur needs API 31 | **Fake glass only** (gradients, rim, sheen) |
| Frosted glass cards | Readable over wallpapers | Low | **Use** for dock band and pills |
| Neon gaming | Fits My Mode / PS5 | Busy if overused | Theme-level (Gaming, Neon Coast) |
| PlayStation-inspired | Fits PS5 shortcut | Trademark | Colour mood only, no Sony logos or art |
| Minimal dark dashboard | Father Mode clarity | Low | Father Mode: fewer, larger tiles |

**Recommended combination: FUTURE CINEMA OS** = Apple TV-style top shelf + frosted dock with one springing glass highlight + aurora glow tinted by the focused app + licensed wallpapers and festival themes, with Lite Mode as the safety valve.
