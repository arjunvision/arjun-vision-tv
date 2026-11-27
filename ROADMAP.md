# Roadmap

## Done (v0.2.0)
Phases 1-4 of the original brief, the Online Theme Studio module, and the FUTURE CINEMA OS redesign. See FEATURES.md for what was tested on the TV.

## Next
1. **Play-ready signing**: the release APK is signed with this Mac's debug key (fine for sideloading). A real upload key is needed only for a store.
2. **Default Home on MiTV-MOOR2**: decide with you. Options, all reversible but all change the TV:
   a. Keep as is (open from the Apps row). Nothing changed.
   b. Disable `com.google.android.tungsten.setupwraith` (check it doesn't own Wi-Fi first), then choose ARJUN VISION TV as Home.
   c. Accessibility helper that redirects the Home key (CleanYourTV's approach).
   Needs your explicit approval before any `pm disable-user`.
3. ~~**System screensaver**: a `DreamService` version of the clock so Android's own screensaver can use it.~~ Built in Phase 8 (`ArjunDreamService`).
4. **Hold-to-move in the dock** directly on Home (today: App library → hold OK → Move left/right).
5. **Pexels / Pixabay keys**: add free keys to `local.properties` for more inspiration variety.
6. **Festival dates**: check Diwali, Pongal and Tamil New Year dates yearly against the Tamil Nadu government holiday list (`FestivalCalendar.kt`).
7. Custom wallpaper from a USB stick or the TV's own photos (needs a storage permission, so ask first).

## Not planned
- Remote control of the PS5 (no supported public interface).
- Debloating Xiaomi packages (only with your explicit approval, and never `com.mitv.livetv`).
- Scraping websites or using unlicensed movie or game art.
