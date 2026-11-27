#!/bin/bash
# Undo for "ARJUN VISION TV as default Home" on the Mi TV MiTV-MOOR2.
# Re-enables the two Google packages that were switched off (nothing was uninstalled)
# and brings back Google TV's own home screen.
#
# Recorded before the change on 2026-11-15:
#   com.google.android.apps.tv.launcherx      enabled (state 0)  HOME priority 2
#   com.google.android.tungsten.setupwraith   enabled (state 0)  HOME priority 1
#   Home resolved to: com.google.android.apps.tv.launcherx/.home.HomeActivity
#
# Usage:  ./tools/restore-google-tv-home.sh [TV_IP:PORT]

set -u
TV="${1:-${TV:?usage: restore-google-tv-home.sh TV_IP:5555 (or set TV)}}"
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"

"$ADB" connect "$TV" >/dev/null
"$ADB" -s "$TV" shell pm enable --user 0 com.google.android.tungsten.setupwraith
"$ADB" -s "$TV" shell pm enable --user 0 com.google.android.apps.tv.launcherx
"$ADB" -s "$TV" shell cmd package set-home-activity com.google.android.apps.tv.launcherx/.home.HomeActivity
"$ADB" -s "$TV" shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
echo "Home now resolves to:"
"$ADB" -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1
