#!/usr/bin/env bash
set -euo pipefail
ND="$(cd "$(dirname "$0")/.." && pwd)"
TC="${ASK_TOOLCHAIN:-$(cd "$ND/../.." && pwd)/toolchain}"
JDK="$(find "$TC" -maxdepth 1 -type d -name 'jdk-21*' | head -1)"
APK="${1:?Usage: apply.sh ORIGINAL_APK CAPTIONS_MPP}"
CAPTIONS="${2:?Captions 0.4.0 MPP required}"
"$JDK/bin/java" -Xmx3g -jar "$TC/desktop.jar" patch "$APK" \
 -p "$TC/official.mpp" -e 'Ask flyout menu' -e 'Hide video action buttons' \
 -p "$CAPTIONS" -e 'Universal auto-translated captions' \
 -p "$ND/dist/Ask_Native_ActionBar_0.6.2.mpp" -e 'ZZ Ask native action button' \
 --exclusive --unsigned --validate -o "$ND/build/test-output.apk" -r "$ND/logs/test-apply-result.json"
