#!/usr/bin/env bash
set -euo pipefail
if [[ $# != 2 ]]; then
    echo "Usage: $0 <toolchain-directory> <output.mpp>" >&2
    exit 2
fi
source_dir=$(cd "$(dirname "$0")" && pwd)
toolchain=$(realpath "$1")
output=$(realpath -m "$2")
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT
mkdir -p "$build"/{classes,dex} "$(dirname "$output")"

kotlin_cp="$toolchain/kotlin-compiler-embeddable-2.4.jar:$toolchain/kotlin-stdlib-2.4.jar:$toolchain/kotlin-script-runtime-2.4.jar:$toolchain/kotlin-reflect.jar:$toolchain/kotlin-coroutines.jar:$toolchain/annotations.jar"
java -cp "$kotlin_cp" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -jvm-target 11 -no-stdlib -no-reflect -module-name nesquik_firefox_blocker \
    -classpath "$toolchain/morphe.jar:$toolchain/kotlin-stdlib-2.4.jar" \
    -d "$build/classes" "$source_dir"/src/*.kt
(cd "$build/classes" && zip -qr "$build/classes.jar" .)
java -cp "$toolchain/r8.jar" com.android.tools.r8.D8 --release --min-api 29 \
    --lib "$toolchain/android.jar" --classpath "$toolchain/morphe.jar" \
    --output "$build/dex" "$build/classes.jar"
cp "$build/dex/classes.dex" "$build/classes/"
cp "$source_dir/src/LocalWebsiteBlocker.sys.mjs" "$build/classes/"
cp "$source_dir/MANIFEST.MF" "$build/classes/META-INF/"
(cd "$build/classes" && zip -qr "$build/patch.mpp" .)
unzip -tq "$build/patch.mpp"
mv "$build/patch.mpp" "$output"
