#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
for version in 21.30.216 21.44.528; do
 toolchain/jdk-21.0.13+9/bin/java -Xmx3g -jar toolchain/desktop-current.jar patch "recovery/youtube-$version.apk" -p toolchain/official-current.mpp -e 'Ask flyout menu' -e 'Hide video action buttons' -p recovery/universal-auto-translated-captions-0.4.0.mpp -e 'Universal auto-translated captions' -p project/native/dist/Ask_Native_ActionBar_0.8.0.mpp -e 'ZZ Ask native action button' --exclusive --unsigned --validate -o "project/native/build/080-$version.apk" -r "project/native/logs/result-080-$version.json" > "project/native/logs/apply-080-$version.log" 2>&1
done
