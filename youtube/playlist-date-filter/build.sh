#!/usr/bin/env bash
set -euo pipefail
if [[ $# != 2 ]]; then
    echo "Usage: $0 <toolchain-directory> <output.mpp>" >&2
    exit 2
fi
root=$(cd "$(dirname "$0")" && pwd)
tools=$(realpath "$1")
output=$(realpath -m "$2")
scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT
mkdir -p "$scratch"/{jvm,app,archive/extensions}
mkdir -p "$(dirname "$output")"

# Produce compile-only JVM bindings from the pinned official Android extension.
# The official extension remains supplied by Morphe, and none of these classes is bundled twice.
unzip -q "$tools/dex-tools.zip" -d "$scratch"
for part in shared shared-youtube youtube; do
    unzip -p "$tools/official-playlist.mpp" "extensions/$part.mpe" > "$scratch/$part.dex"
    bash "$scratch/dex-tools-v2.4/d2j-dex2jar.sh" -o "$scratch/$part.jar" "$scratch/$part.dex"
done
bindings="$scratch/shared.jar:$scratch/shared-youtube.jar:$scratch/youtube.jar"

javac --release 17 -cp "$tools/android.jar:$tools/json.jar:$bindings:$tools/annotations.jar" \
    -d "$scratch/app" "$root"/src/*.java
jar cf "$scratch/app.jar" -C "$scratch/app" .
java -cp "$tools/r8.jar" com.android.tools.r8.D8 --release --min-api 29 \
    --lib "$tools/android.jar" \
    --output "$scratch" "$scratch/app.jar"
mv "$scratch/classes.dex" "$scratch/archive/extensions/nesquik-playlist-date.dex"

compiler="$tools/kotlin-compiler-embeddable-2.4.jar:$tools/kotlin-stdlib-2.4.jar:$tools/kotlin-reflect.jar"
compiler+=":$tools/kotlin-script-runtime-2.4.jar:$tools/kotlin-coroutines.jar:$tools/annotations.jar"
java -cp "$compiler" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -no-stdlib -no-reflect -Xcontext-parameters -jvm-target 17 \
    -classpath "$tools/morphe.jar:$tools/kotlin-stdlib-2.4.jar:$tools/annotations.jar" \
    -d "$scratch/jvm" "$root"/src/*.kt
jar cf "$scratch/patch.jar" -C "$scratch/jvm" .
java -cp "$tools/r8.jar" com.android.tools.r8.D8 --release --min-api 29 \
    --lib "$tools/android.jar" --classpath "$tools/morphe.jar" \
    --output "$scratch/archive" "$scratch/patch.jar"
cp -r "$scratch/jvm/." "$scratch/archive/"
cp "$root/LICENSE" "$root/NOTICE" "$scratch/archive/"
jar cfm "$output" "$root/MANIFEST.MF" -C "$scratch/archive" .
unzip -tq "$output"
