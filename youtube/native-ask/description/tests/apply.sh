#!/usr/bin/env bash
set -euo pipefail
TASK="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$TASK"
APK="$(find recovery -maxdepth 1 -name '*.apk' | head -1)"
ASK="$(find recovery -maxdepth 1 -name 'Ask_Native_ActionBar_0.8.2*.mpp' | head -1)"
CAP="recovery/universal-auto-translated-captions-0.4.0.mpp"
java -Xmx3g -jar toolchain/desktop.jar patch "$APK" -p toolchain/official-current.mpp -e 'Ask flyout menu' -e 'Hide video action buttons' -p "$ASK" -e 'ZZ Ask native action button' -p "$CAP" -e 'Universal auto-translated captions' -p project/description/dist/Native_Gemini_Description_0.1.0_experimental.mpp -e 'Force native Gemini summary button (Description)' --exclusive --unsigned --validate -o project/description/build/2130-combined.apk -r project/description/logs/apply-combined.json > project/description/logs/apply-combined.log 2>&1
