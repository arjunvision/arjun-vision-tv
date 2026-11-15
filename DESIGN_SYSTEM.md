# ARJUN VISION design system: FUTURE CINEMA OS

Code: `ui/theme/Theme.kt` (tokens), `ui/components/*` (building blocks), `domain/themes/ThemeFactory.kt` (theme designs).

## Colour tokens (default theme "Arjun Vision Classic")

| Token | Value | Use |
|---|---|---|
| `LauncherColors.Background` | `#070910` OLED Black | Screen background |
| `LauncherColors.Surface` | `#101522` Deep Navy | Cards, panels |
| `LauncherColors.Blue` | `#00B8FF` Electric Blue | Primary accent, focus, glow |
| `LauncherColors.Violet` | `#8B5CF6` Neon Violet | Secondary accent, aurora |
| `LauncherColors.TextPrimary` | `#F8FAFC` Soft White | Text |
| `LauncherColors.TextSecondary` | `#A1A1AA` | Secondary text |
| `GlassFill` / `GlassEdge` | white 12 % / 20 % | Frosted glass (no blur) |
| Status | green `#22C55E`, amber `#F59E0B`, red `#EF4444` | Network state only. Never themed |

Colour tokens are Compose snapshot state: applying a theme repaints every screen without a restart.

## Shape, motion, type tokens (`LauncherStyle`)

| Token | Default | Range in themes |
|---|---|---|
| `cardCornerDp` | 24 | 10 to 36 |
| `focusScale` | 0.06 (6 % zoom) | 0.05 to 0.08 |
| `titleWeight` | 700 | 300 (Cinema) to 900 (Gaming) |
| `titleLetterSpacing` | 0 sp | 0 to 3 sp |
| `particles` | STARS | NONE, STARS, SNOW, SPARKS, RAIN, PETALS |
| `lite` | false | Lite Mode switch |

Springs: tile zoom `dampingRatio 0.62, stiffness MediumLow`; glass highlight `0.62 / 300`. Animation intensity (Off, Subtle, Normal, Full) multiplies zoom and turns motion off at "Off".

## Components

| Component | File | Notes |
|---|---|---|
| `FocusCard` | `FocusCard.kt` | Every remote-focusable surface. OK = click; hold OK ≥ 550 ms or MENU = menu |
| `GlassDock` + `GlassHighlightState` | `GlassDock.kt` | Frosted band + ONE floating glass highlight that springs between tiles, rim tinted by the app colour |
| `AuroraBackground` | `AmbientLayers.kt` | 3 drifting radial colour fields; time-of-day strength; static in Lite Mode |
| `ParticleLayer` | `AmbientLayers.kt` | 36 particles max, themed |
| `ThemeWallpaper` | `AmbientLayers.kt` | Licensed photo, cross-fade on change, dark scrim, RGB_565, max 1920 px |
| `rememberAppVisual` / `AppIcon` | `FocusCard.kt` | Icon + vivid colour for ambient glow, cached |
| `LogoMark` | `FocusCard.kt` | The Arjun Vision bulb logo from `brand-assets` |

## Layout (1920×1080 = 960×540 dp on the TV)

- 48 dp side margins, 24 dp top and bottom (TV overscan safe).
- Header: logo, ARJUN VISION, clock + date, network pill, profile, Settings.
- Top shelf: greeting (time of day), focused tile name at 50 sp, subtitle.
- Dock: frosted band, tiles 200/230/260 dp wide (Father Mode at least 250 dp), 0.6 aspect.
- Credit line for licensed wallpapers (required by CC BY / CC BY-SA).

## Themes

15 hand-made base designs, one per category (Classic, Christmas, New Year, Pongal, Tamil New Year, Diwali, Independence Day, Summer, Monsoon, Gaming, PlayStation Night, Neon Coast (Vice City-inspired), AI & Future Tech, Cinema, Special Event).
A generated theme keeps the category's primary accent (so it stays ARJUN VISION) and takes a tint, wallpaper and corner variation from one licensed photo. Neon Coast and PlayStation Night use colour moods only: no game art, logos or trademarks.
