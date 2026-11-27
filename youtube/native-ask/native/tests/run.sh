#!/usr/bin/env bash
set -euo pipefail
ND="$(cd "$(dirname "$0")/.." && pwd)"
TC="${ASK_TOOLCHAIN:-$(cd "$ND/../.." && pwd)/toolchain}"
JDK="$(find "$TC" -maxdepth 1 -type d -name 'jdk-21*' | head -1)"
mkdir -p "$ND/build/tests"
"$JDK/bin/javac" --release 11 -cp "$ND/build/classes" -d "$ND/build/tests" $(find "$ND/tests/stubs" -name '*.java') "$ND/tests/RouteTest.java" "$ND/tests/NativeProtoTest.java" "$ND/tests/OriginalVisualPanelTest.java" "$ND/tests/PanelPresentationTest.java" "$ND/tests/FullscreenQuickActionsTest.java" "$ND/tests/CompactStyleTest.java" "$ND/tests/TabletMenuTest.java" "$ND/tests/MenuCaptureTest.java" "$ND/tests/MinimalLayoutTest.java"
"$JDK/bin/java" -cp "$ND/build/tests:$ND/build/classes" app.neszi.asknative.NativeProtoTest
"$JDK/bin/java" -cp "$ND/build/tests:$ND/build/classes" app.neszi.asknative.RouteTest

"$JDK/bin/java" -cp "$ND/build/tests:$ND/build/classes" app.neszi.asknative.OriginalVisualPanelTest

"$JDK/bin/java" -cp "$ND/build/tests:$ND/build/classes" app.neszi.asknative.PanelPresentationTest

"$JDK/bin/java" -cp "$ND/build/tests:$ND/build/classes" app.neszi.asknative.FullscreenQuickActionsTest

"$JDK/bin/java" -cp "$ND/build/tests:$ND/build/classes" app.neszi.asknative.CompactStyleTest

"$JDK/bin/java" -cp "$ND/build/tests:$ND/build/classes" app.neszi.asknative.TabletMenuTest

"$JDK/bin/java" -cp "$ND/build/tests:$ND/build/classes" app.neszi.asknative.MenuCaptureTest

"$JDK/bin/java" -cp "$ND/build/tests:$ND/build/classes" app.neszi.asknative.MinimalLayoutTest

"$JDK/bin/javac" --release 11 -cp "$ND/build/classes:$TC/android.jar" -d "$ND/build/tests" "$ND/tests/WatchKeyboardTest.java"
"$JDK/bin/java" -cp "$ND/build/tests:$ND/build/classes:$TC/android.jar" app.neszi.asknative.WatchKeyboardTest
