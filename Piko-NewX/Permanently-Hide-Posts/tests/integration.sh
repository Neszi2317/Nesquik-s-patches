#!/usr/bin/env bash
set -euo pipefail
# Usage: integration.sh TOOLCHAIN INPUT.apkm OUTPUT_DIR [also-test-custom.1.mpp]
d=$(cd "$(dirname "$0")/.." && pwd)
root=$(cd "$d/../.." && pwd)
tc=$(realpath "$1"); input=$(realpath "$2"); mkdir -p "$3"; out=$(realpath "$3")
new=(-p "$d/Permanently-Hide-Posts-0.1.0.mpp" -e 'NewX: Permanently Hide Posts')
piko=(-p "$tc/piko.mpp" -e 'NewX: Filter posts by keyword' -e 'NewX: Browse tweet object')
personal=(-p "$root/bundles/Nesquiks-patches-0.2.0.mpp")
features=(-e 'NewX: Advanced Global Keyword Filter' -e 'NewX: Hide all sensitive posts' -e 'NewX: Anonymous Names' -e 'NewX: Hide Profile Pictures & Banners' -e 'NewX: In-App Media Blur')
run(){ local name=$1; shift
    java -XX:ActiveProcessorCount=4 -Xmx3g -jar "$tc/morphe.jar" patch --exclusive "$@" -o "$out/$name.apk" -r "$out/$name.json" "$input" >"$out/$name.log" 2>&1
    python3 -c 'import json,sys; sys.exit(0 if json.load(open(sys.argv[1]))["success"] else 1)' "$out/$name.json"
}
run combined "${piko[@]}" "${personal[@]}" "${features[@]}" "${new[@]}"
run reversed "${new[@]}" "${personal[@]}" "${features[@]}" "${piko[@]}"
run minimal "${piko[@]}" "${new[@]}"
if [[ -n ${4:-} ]]; then run custom -p "$4" -e 'NewX: Browse tweet object' -e 'NewX: Filter posts by keyword' "${features[@]}" "${new[@]}"; fi
if run missing "${new[@]}";then echo 'unexpected success without Piko'; exit 1;fi
grep -F 'Select Piko' "$out/missing.log" >/dev/null
for feature in 'Browse tweet object' 'Filter posts by keyword';do
    slug=${feature%% *}
    if run "missing-$slug" -p "$tc/piko.mpp" -e "NewX: $feature" "${new[@]}";then echo "unexpected success with only $feature";exit 1;fi
    grep -F 'Select Piko' "$out/missing-$slug.log" >/dev/null
done
