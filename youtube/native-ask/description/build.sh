#!/usr/bin/env bash
set -euo pipefail
ND="$(cd "$(dirname "$0")" && pwd)"
TC="${ASK_TOOLCHAIN:-$(cd "$ND/../.." && pwd)/toolchain}"
JDK="$(find "$TC" -maxdepth 1 -type d -name 'jdk-21*' | head -1)"
BUILD="$ND/build"
rm -rf "$BUILD/classes" "$BUILD/patches" "$BUILD/bundle"
mkdir -p "$BUILD/classes" "$BUILD/patches" "$BUILD/bundle/extensions" "$ND/dist"
"$JDK/bin/javac" --release 11 -cp "$TC/android.jar" -d "$BUILD/classes" "$ND/src/DescriptionNative.java" "$ND/src/Wire.java"
"$JDK/bin/jar" cf "$BUILD/helper.jar" -C "$BUILD/classes" .
"$JDK/bin/java" -cp "$TC/r8.jar" com.android.tools.r8.D8 --release --min-api 28 --lib "$TC/android.jar" --output "$BUILD/bundle/extensions" "$BUILD/helper.jar"
mv "$BUILD/bundle/extensions/classes.dex" "$BUILD/bundle/extensions/description-native.dex"
"$JDK/bin/java" -cp "$TC/kotlin.jar:$TC/desktop.jar:$TC/annotations.jar" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -jvm-target 17 -classpath "$TC/desktop.jar:$TC/annotations.jar" -d "$BUILD/patches" "$ND/src/DescriptionPatch.kt"
"$JDK/bin/jar" cf "$BUILD/patch.jar" -C "$BUILD/patches" .
"$JDK/bin/java" -cp "$TC/r8.jar" com.android.tools.r8.D8 --release --min-api 28 --lib "$TC/android.jar" --classpath "$TC/desktop.jar" --output "$BUILD/bundle" "$BUILD/patch.jar"
cp -r "$BUILD/patches/"* "$BUILD/bundle/"
cat > "$BUILD/MANIFEST.MF" <<MANIFEST
Manifest-Version: 1.0
Name: Native Gemini Description Persistence
Description: Experimental preservation of original footer for previously captured videos
Version: 0.1.0
Patcher-Version: 2.0.0
Author: Neszi2317
License: GPLv3

MANIFEST
"$JDK/bin/jar" cfm "$ND/dist/Native_Gemini_Description_0.1.0_experimental.mpp" "$BUILD/MANIFEST.MF" -C "$BUILD/bundle" .
python3 "$ND/tests/verify_bundle.py" "$ND/dist/Native_Gemini_Description_0.1.0_experimental.mpp"
