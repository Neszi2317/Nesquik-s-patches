#!/usr/bin/env bash
set -euo pipefail

if [[ $# != 4 ]]; then
    echo "Usage: $0 <base-Piko.mpp> <Morphe-desktop.jar> <tool-directory> <output.mpp>" >&2
    exit 2
fi
source_dir=$(cd "$(dirname "$0")" && pwd)
base=$(realpath "$1")
morphe=$(realpath "$2")
tools=$(realpath "$3")
output=$(realpath -m "$4")
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT

mkdir -p "$build"/{runtime,patch,bundle-dex,extensions}
mapfile -d '' -t runtime < <(find "$source_dir/runtime" "$source_dir/stubs" -name '*.java' -print0)
java -m jdk.compiler/com.sun.tools.javac.Main -source 17 -target 17 \
    -cp "$tools/android.jar:$tools/kotlin-stdlib-2.4.jar" -d "$build/runtime" "${runtime[@]}"

kotlin_cp="$tools/kotlin-compiler-embeddable-2.4.jar:$tools/kotlin-stdlib-2.4.jar:$tools/kotlin-script-runtime-2.4.jar:$tools/kotlin-reflect.jar:$tools/kotlin-coroutines.jar:$tools/annotations.jar"
mapfile -d '' -t patches < <(find "$source_dir/patch" "$source_dir/vendor" -name '*.kt' -print0)
java -Xmx2g -cp "$kotlin_cp" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -no-stdlib -no-reflect -jvm-target 11 -Xcontext-parameters -module-name piko_custom_x1243 \
    -Xfriend-paths="$base" -classpath "$morphe:$base:$build/runtime:$tools/kotlin-stdlib-2.4.jar" \
    -d "$build/patch" "${patches[@]}"

java -m jdk.compiler/com.sun.tools.javac.Main -source 11 -target 11 \
    -cp "$morphe:$base:$build/patch" -d "$build/patch" "$source_dir/patch/InAppMediaBlurPatch.java"

python3 "$source_dir/package.py" prepare "$base" "$build" "$output"
for ext in advanced anonymous profile-images hide-sensitive media-blur; do
    [[ -f "$build/extensions/$ext.jar" ]] || continue
    mkdir -p "$build/extensions/$ext"
    java -Xmx1g -cp "$tools/r8.jar" com.android.tools.r8.D8 --release --min-api 29 \
        --lib "$tools/android.jar" --classpath "$build/runtime-stubs.jar" \
        --classpath "$tools/kotlin-stdlib-2.4.jar" --output "$build/extensions/$ext" "$build/extensions/$ext.jar"
done
java -Xmx2g -cp "$tools/r8.jar" com.android.tools.r8.D8 --release --min-api 29 \
    --lib "$tools/android.jar" --classpath "$morphe" --output "$build/bundle-dex" "$build/patch.jar"
python3 "$source_dir/package.py" finish "$base" "$build" "$output"
