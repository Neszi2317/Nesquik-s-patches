#!/usr/bin/env bash
set -euo pipefail
NATIVE_DIR="$(cd "$(dirname "$0")" && pwd)"
TASK_ROOT="$(cd "$NATIVE_DIR/../.." && pwd)"
TC="${ASK_TOOLCHAIN:-$TASK_ROOT/toolchain}"
JDK="$(find "$TC" -maxdepth 1 -type d -name 'jdk-21*' | head -1)"
BUILD="$NATIVE_DIR/build"
# Prevent stale unit-test classes or prior bundle entries entering the deliverable.
rm -rf "$BUILD/classes" "$BUILD/patches" "$BUILD/bundle"
mkdir -p "$BUILD/classes" "$BUILD/patches" "$BUILD/bundle/extensions" "$NATIVE_DIR/dist"
"$JDK/bin/javac" --release 11 -cp "$TC/android.jar" -d "$BUILD/classes" "$NATIVE_DIR/src/AskNative.java"
"$JDK/bin/jar" cf "$BUILD/helper.jar" -C "$BUILD/classes" .
"$JDK/bin/java" -cp "$TC/r8.jar" com.android.tools.r8.D8 --release --min-api 28 --lib "$TC/android.jar" --output "$BUILD/bundle/extensions" "$BUILD/helper.jar"
mv "$BUILD/bundle/extensions/classes.dex" "$BUILD/bundle/extensions/ask-native.dex"
"$JDK/bin/java" -cp "$TC/kotlin.jar:$TC/desktop.jar:$TC/annotations.jar" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -jvm-target 17 -classpath "$TC/desktop.jar:$TC/annotations.jar" -d "$BUILD/patches" "$NATIVE_DIR/src/AskNativePatch.kt"
"$JDK/bin/jar" cf "$BUILD/patch.jar" -C "$BUILD/patches" .
"$JDK/bin/java" -cp "$TC/r8.jar" com.android.tools.r8.D8 --release --min-api 28 --lib "$TC/android.jar" --classpath "$TC/desktop.jar" --output "$BUILD/bundle" "$BUILD/patch.jar"
cp -r "$BUILD/patches/"* "$BUILD/bundle/"
cat > "$BUILD/MANIFEST.MF" <<MANIFEST
Manifest-Version: 1.0
Name: Ask Native Action Bar
Description: Native Elements action button with official Ask panel opening
Version: 0.8.2
Patcher-Version: 2.0.0
Author: Neszi2317
License: GPLv3
Source: https://github.com/Neszi2317/Rounded-Subtiles

MANIFEST
"$JDK/bin/jar" cfm "$NATIVE_DIR/dist/Ask_Native_ActionBar_0.8.2.mpp" "$BUILD/MANIFEST.MF" -C "$BUILD/bundle" .
# Manager's JVM host is Java17 even on recent Android. Do not regress to Java21.
python3 "$NATIVE_DIR/tests/verify_java17.py" "$NATIVE_DIR/dist/Ask_Native_ActionBar_0.8.2.mpp"
