#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
tc=$(realpath "${1:?toolchain directory}")
out=$(realpath -m "${2:-../releases/Cross-Device-Detox-Sync-0.1.1.mpp}")
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
mkdir -p "$work"/{stubs,runtime,jvm,bundle/extensions,extdex}
javac --release 11 -cp "$tc/android.jar" -d "$work/stubs" $(find stubs -name '*.java')
jar cf "$work/stubs.jar" -C "$work/stubs" .
javac --release 11 -cp "$tc/android.jar:$work/stubs.jar" -d "$work/runtime" runtime/*.java
jar cf "$work/runtime.jar" -C "$work/runtime" .
java -cp "$tc/r8.jar" com.android.tools.r8.D8 --release --min-api 24 --lib "$tc/android.jar" --classpath "$work/stubs.jar" --output "$work/extdex" "$work/runtime.jar"
cp "$work/extdex/classes.dex" "$work/bundle/extensions/nesquik-detox.mpe"
java -cp "$tc/*" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -jvm-target 11 -classpath "$tc/morphe.jar:$tc/kotlin-stdlib-2.4.jar" -d "$work/jvm" patch/*.kt
jar cf "$work/patch.jar" -C "$work/jvm" .
java -cp "$tc/r8.jar" com.android.tools.r8.D8 --release --min-api 29 --lib "$tc/android.jar" --classpath "$tc/morphe.jar" --output "$work/bundle" "$work/patch.jar"
cp -r "$work/jvm/." "$work/bundle/"
jar cfm "$out" MANIFEST.MF -C "$work/bundle" .
