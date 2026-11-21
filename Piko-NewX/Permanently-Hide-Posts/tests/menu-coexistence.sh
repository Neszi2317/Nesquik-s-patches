#!/usr/bin/env bash
set -euo pipefail
tc=$(realpath "$1"); input=$(realpath "$2"); mkdir -p "$3"; out=$(realpath "$3")
d=$(cd "$(dirname "$0")/.." && pwd)
java -XX:ActiveProcessorCount=4 -Xmx3g -jar "$tc/morphe.jar" patch --exclusive \
  -p "$tc/piko.mpp" -e 'NewX: Browse tweet object' -e 'NewX: Filter posts by keyword' \
  -e 'NewX: Customize post menu items' -e 'NewX: Translator' \
  -p "$d/Permanently-Hide-Posts-0.1.0.mpp" -e 'NewX: Permanently Hide Posts' \
  -o "$out/menu.apk" -r "$out/menu.json" "$input" >"$out/menu.log" 2>&1
