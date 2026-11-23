#!/usr/bin/env bash
set -euo pipefail
if [[ $# != 1 ]]; then
    echo "Usage: $0 <tool-directory>" >&2
    exit 2
fi
tools=$(realpath "$1")
cd "$(dirname "$0")"
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT
mkdir -p "$build"/{runtime,advanced,anonymous,profile,core,settings,host}

compile() {
    java -m jdk.compiler/com.sun.tools.javac.Main -source 17 -target 17 -Xlint:-options "$@"
}
env_cp="$tools/android.jar:$tools/kotlin-stdlib-2.4.jar"
mapfile -d '' -t runtime < <(find runtime stubs -name '*.java' -print0)
compile -cp "$env_cp" -d "$build/runtime" "${runtime[@]}"
runtime_cp="$build/runtime:$env_cp"
compile -cp "$env_cp" -d "$build/host" host-stubs/android/content/*.java
runtime_cp="$build/host:$runtime_cp"
compile -cp "$runtime_cp" -d "$build/core" blur-tests/*.java SensitivePostFilterHostTest.java
for test in SessionStateTest PostIdentityTest app.morphe.extension.newx.hidesensitive.SensitivePostFilterHostTest; do
    java -cp "$build/core:$runtime_cp" "$test"
done

mkdir -p "$build/blur"
mapfile -d '' -t blur_fixtures < <(find blur-tests/fixtures -name '*.java' -print0)
compile -cp "$build/core:$runtime_cp" -d "$build/blur" "${blur_fixtures[@]}"
blur_cp="$build/blur:$build/core:$runtime_cp"
java -cp "$blur_cp" BlurScopeTest
if java -Dblur.test.beforeReplyScope=true -cp "$blur_cp" BlurScopeTest > "$build/blur-before.log" 2>&1; then
    echo "The reply regression test must fail when the old render route bypasses the scope." >&2
    exit 1
fi
grep -Fq 'AssertionError: Reply body receives the blur modifier' "$build/blur-before.log"
echo 'PASS the old reply render route reproduces unblurred media after the button press'

mapfile -d '' -t advanced < <(find advanced-tests/app -name '*.java' -print0)
compile -cp "$runtime_cp" -d "$build/advanced" "${advanced[@]}"
java -cp "$build/advanced:$tools/json.jar:$runtime_cp" app.morphe.extension.newx.postfilter.AdvancedFilterTests

asm="$tools/kotlin-compiler-embeddable-2.4.jar"
mapfile -d '' -t anonymous < <(find anonymous-tests -name '*.java' ! -name '*DexAudit.java' -print0)
compile -cp "$runtime_cp:$asm" -d "$build/anonymous" "${anonymous[@]}"
name_dir=app/morphe/extension/newx/anonymous
java -cp "$build/anonymous:$asm" TestBridges "$build/runtime/$name_dir/Names.class" "$build/anonymous/$name_dir/Names.class"
java -cp "$build/anonymous:$runtime_cp" app.morphe.extension.newx.anonymous.AliasEngineTests
for phase in first second; do
    java -cp "$build/anonymous:$runtime_cp" app.morphe.extension.newx.anonymous.HostRuntimeTests "$build/settings" "$phase"
done

mapfile -d '' -t profile < <(find profile-tests -name '*.java' ! -name '*DexAudit.java' ! -name 'Inspect.java' -print0)
compile -cp "$runtime_cp:$asm" -d "$build/profile" "${profile[@]}"
profile_dir=app/morphe/extension/newx/profileimages
java -cp "$build/profile:$asm" TestBridges "$build/runtime/$profile_dir/ProfileImages.class" "$build/profile/$profile_dir/ProfileImages.class"
for phase in first restart; do
    java -cp "$build/profile:$runtime_cp" RuntimeTests "$build/settings" "$phase"
done
