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
mkdir -p "$scratch/jvm" "$scratch/dex" "$(dirname "$output")"
legacy="$root/../../releases/Rounded-Captions-1.0.0-dev.6.mpp"

compiler="$tools/kotlin-compiler-embeddable-2.4.jar:$tools/kotlin-stdlib-2.4.jar:$tools/kotlin-reflect.jar"
compiler+=":$tools/kotlin-script-runtime-2.4.jar:$tools/kotlin-coroutines.jar:$tools/annotations.jar"
java -cp "$compiler" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -no-stdlib -no-reflect -Xcontext-parameters -jvm-target 17 -module-name rounded_captions \
    -classpath "$tools/morphe.jar:$tools/kotlin-stdlib-2.4.jar:$tools/annotations.jar" \
    -d "$scratch/jvm" "$root/RoundedCaptionsRecentPatch.kt"

python3 - "$legacy" "$scratch" <<'PY'
import re
import sys
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

legacy, scratch = Path(sys.argv[1]), Path(sys.argv[2])
replaced = {
    "patches/youtube/captions/" + name + ".class" for name in (
        "RoundedBackgroundFingerprint2146522",
        "CenteredCaptionFingerprint2146522",
        "RoundedCaptions2146522PatchKt",
    )
}
with ZipFile(legacy) as source, ZipFile(scratch / "classes.jar", "w", ZIP_DEFLATED) as archive:
    if not replaced.issubset(source.namelist()):
        raise ValueError("The original 21.46.522 captions classes are missing")
    for entry in source.namelist():
        if entry in replaced or entry.endswith("/") or re.fullmatch(r"classes\d*\.dex", entry):
            continue
        data = source.read(entry)
        if entry == "META-INF/MANIFEST.MF":
            data = data.replace(b"1.0.0-dev.6", b"1.0.0-dev.7")
        archive.writestr(entry, data)
    jvm = scratch / "jvm"
    for path in sorted(jvm.rglob("*")):
        if path.is_file():
            archive.write(path, path.relative_to(jvm))
PY

java -cp "$tools/r8.jar" com.android.tools.r8.D8 --release --min-api 29 \
    --lib "$tools/android.jar" --classpath "$tools/morphe.jar" \
    --output "$scratch/dex" "$scratch/classes.jar"
python3 - "$scratch" "$output" <<'PY'
import sys
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

scratch, output = Path(sys.argv[1]), Path(sys.argv[2])
with ZipFile(scratch / "classes.jar") as source, ZipFile(output, "w", ZIP_DEFLATED) as archive:
    for entry in source.namelist():
        archive.writestr(entry, source.read(entry))
    for path in sorted((scratch / "dex").glob("classes*.dex")):
        archive.write(path, path.name)
PY
unzip -tq "$output"
