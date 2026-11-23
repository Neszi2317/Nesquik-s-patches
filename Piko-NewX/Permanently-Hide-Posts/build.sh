#!/usr/bin/env bash
set -euo pipefail
d=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$d/../.." && pwd)
tc=$(realpath "${1:-$root/.toolchain}")
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT
mkdir -p "$build"/{stubs,ext,jvm,bundle,rdex,pdex}
jc=(java -m jdk.compiler/com.sun.tools.javac.Main -source 11 -target 11)
mapfile -t stubs < <(find "$d/stubs" -name '*.java')
"${jc[@]}" -cp "$tc/android.jar" -d "$build/stubs" "${stubs[@]}" "$root/x/stubs/app/morphe/extension/shared/Utils.java" "$root/x/stubs/app/morphe/extension/crimera/settings/CustomScreenFragment.java"
"${jc[@]}" -cp "$tc/android.jar:$build/stubs" -d "$build/ext" "$d"/runtime/*.java
zipclasses(){ (cd "$1" && zip -qr "$2" .); }
zipclasses "$build/stubs" "$build/stubs.jar"
zipclasses "$build/ext" "$build/ext.jar"
java -cp "$tc/*" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -jvm-target 11 -no-stdlib -no-reflect -classpath "$tc/morphe.jar:$tc/kotlin-stdlib-2.4.jar" -d "$build/jvm" "$d/patch/Hidden.kt"
zipclasses "$build/jvm" "$build/patch.jar"
d8=(java -cp "$tc/r8.jar" com.android.tools.r8.D8 --release --min-api 28 --lib "$tc/android.jar")
"${d8[@]}" --classpath "$build/stubs.jar" --output "$build/rdex" "$build/ext.jar"
"${d8[@]}" --classpath "$tc/morphe.jar" --output "$build/pdex" "$build/patch.jar"
cp -r "$build/jvm"/. "$build/bundle/"
mkdir -p "$build/bundle/extensions"
cp "$build/rdex/classes.dex" "$build/bundle/extensions/nesquik-hidden.mpe"
cp "$build/pdex/classes.dex" "$build/bundle/"
cp "$d/MANIFEST.MF" "$build/bundle/META-INF/MANIFEST.MF"
zipclasses "$build/bundle" "$build/complete.mpp"
version=$(sed -n 's/^Version: //p' "$d/MANIFEST.MF")
mv "$build/complete.mpp" "$d/Permanently-Hide-Posts-$version.mpp"
