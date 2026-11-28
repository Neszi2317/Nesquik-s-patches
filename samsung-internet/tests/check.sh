#!/usr/bin/env bash
set -euo pipefail
base=$(cd "$(dirname "$0")" && pwd)
tc=$(realpath "${1:?toolchain directory}")
apk=$(realpath "${2:?APK to check}")
variant=${3:-cases}
scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT
unzip -q "$tc/dex-tools.zip" -d "$scratch"
d2j="$scratch/dex-tools-v2.4"
asm="$d2j/lib/asm-9.5.jar"
mkdir "$scratch/harness"
javac -cp "$tc/morphe.jar:$asm" -d "$scratch/harness" "$base"/*.java
java -Xmx1g -cp "$scratch/harness:$tc/morphe.jar" Extract "$apk" "$scratch/filter.dex"
bash "$d2j/d2j-dex2jar.sh" -o "$scratch/filter.jar" "$scratch/filter.dex"
java -cp "$scratch/harness:$asm:$scratch/filter.jar" Frames "$scratch/filter.jar" "$scratch/framed.jar"
for stock in true false; do
  java -Xverify:all -cp "$scratch/harness:$scratch/framed.jar" Oracle "$stock" "$variant"
done
