#!/usr/bin/env bash
set -euo pipefail
# Usage: integration.sh TOOLCHAIN INPUT.apkm OUTPUT_DIR [COLLECTION.mpp]
d=$(cd "$(dirname "$0")/.." && pwd)
root=$(cd "$d/../.." && pwd)
tc=$(realpath "$1")
input=$(realpath "$2")
mkdir -p "$3"
out=$(realpath "$3")
collection=$(realpath "${4:-$root/bundles/Nesquiks-patches-0.4.1.mpp}")
version=$(sed -n 's/^Version: //p' "$d/MANIFEST.MF")
new=(-p "$d/Permanently-Hide-Posts-$version.mpp" -e 'NewX: Permanently Hide Posts')
keyword='NewX: Filter posts by keyword'
browse='NewX: Browse tweet object'
features=(-e 'NewX: Permanently Hide Posts' -e 'NewX: Advanced Global Keyword Filter'
    -e 'NewX: Hide all sensitive posts' -e 'NewX: Anonymous Names'
    -e 'NewX: Hide Profile Pictures & Banners' -e 'NewX: In-App Media Blur')
recommended=(-p "$tc/piko-dev6.mpp" -d 'NewX: Show sensitive media')
run() {
    local name=$1
    shift
    java -XX:ActiveProcessorCount=4 -Xmx3g -jar "$tc/morphe.jar" patch "$@" \
        -o "$out/$name.apk" -r "$out/$name.json" "$input" >"$out/$name.log" 2>&1
    python3 -c 'import json,sys; sys.exit(0 if json.load(open(sys.argv[1]))["success"] else 1)' "$out/$name.json"
}
run min6 --exclusive -p "$tc/piko-dev6.mpp" -e "$keyword" "${new[@]}"
run default6 "${recommended[@]}" "${new[@]}"
run browse6 --exclusive -p "$tc/piko-dev6.mpp" -e "$keyword" -e "$browse" \
    -e 'NewX: Customize post menu items' -e 'NewX: Translator' "${new[@]}"
run full6 "${recommended[@]}" -p "$collection" "${features[@]}"
run reversed6 -p "$collection" "${features[@]}" "${recommended[@]}"
run dev5 --exclusive -p "$tc/piko.mpp" -e "$keyword" -e "$browse" "${new[@]}"
if run missing6 --exclusive -p "$tc/piko-dev6.mpp" -e "$browse" "${new[@]}"; then
    echo 'The patch unexpectedly succeeded without the Piko keyword filter.' >&2
    exit 1
fi
grep -F "Select Piko's NewX: Filter posts by keyword" "$out/missing6.log" >/dev/null
