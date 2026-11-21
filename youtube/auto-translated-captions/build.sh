#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
# Use fetch-tools.py first. Override paths to reuse a local toolchain.
MORPHE_JAR="${MORPHE_JAR:-$PWD/tools/morphe.jar}"
R8_JAR="${R8_JAR:-$PWD/tools/r8.jar}"
KOTLINC="${KOTLINC:-$PWD/tools/kotlinc/bin/kotlinc}"
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}java"
rm -rf build/classes build/dex
mkdir -p build/classes build/dex
"$KOTLINC" -Xcontext-parameters -Xskip-metadata-version-check -jvm-target 11 \
  -classpath "$MORPHE_JAR" src/main/kotlin/*.kt -d build/classes
python3 scripts/package.py jvm
"$JAVA_BIN" -cp "$R8_JAR" com.android.tools.r8.D8 --min-api 26 \
  --output build/dex build/patch-jvm.jar
python3 scripts/package.py mpp
printf 'Built dist/universal-auto-translated-captions-0.4.0.mpp\n'
