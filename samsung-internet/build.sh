#!/usr/bin/env bash
set -euo pipefail
base=$(cd "$(dirname "$0")" && pwd)
tc=$(realpath "${1:?toolchain directory}")
out=$(realpath -m "${2:?output MPP}")
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT
mkdir -p "$build"/{runtime,dex,bundle/extensions,jvm} "$(dirname "$out")"
javac --release 11 -d "$build/runtime" "$base"/runtime/*.java
jar cf "$build/runtime.jar" -C "$build/runtime" .
java -cp "$tc/r8.jar" com.android.tools.r8.D8 --release --min-api 29 --lib "$tc/android.jar" --output "$build/dex" "$build/runtime.jar"
cp "$build/dex/classes.dex" "$build/bundle/extensions/nesquik-sbrowser-blocker.mpe"
# Share Firefox's TXT reader without shipping duplicate class names in the combined bundle.
sed 's/^package local.nesquikos.firefoxblocker$/package fr.nesquik.patches.sbrowser/' \
  "$base/../firefox/src/DomainRules.kt" > "$build/DomainRules.kt"
java -cp "$tc/*" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -jvm-target 11 \
  -module-name nesquik_sbrowser_blocker -classpath "$tc/morphe.jar:$tc/kotlin-stdlib-2.4.jar" \
  -d "$build/jvm" "$build/DomainRules.kt" "$base"/patch/*.kt
jar cf "$build/patch.jar" -C "$build/jvm" .
java -cp "$tc/r8.jar" com.android.tools.r8.D8 --release --min-api 29 --lib "$tc/android.jar" \
  --classpath "$tc/morphe.jar" --output "$build/bundle" "$build/patch.jar"
cp -r "$build/jvm/." "$build/bundle/"
jar cfm "$out" "$base/MANIFEST.MF" -C "$build/bundle" .
