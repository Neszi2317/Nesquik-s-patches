#!/usr/bin/env bash
set -euo pipefail
if [[ $# != 2 ]]; then
    echo "Usage: $0 <version> <toolchain-directory>" >&2
    exit 2
fi
root=$(cd "$(dirname "$0")/.." && pwd)
version=$1
toolchain=$(realpath "$2")
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || exit 2
scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT
"$root/x/build.sh" "$toolchain/piko.mpp" "$toolchain/morphe.jar" "$toolchain" "$scratch/x.mpp"
hidden="$root/Piko-NewX/Permanently-Hide-Posts"
"$hidden/build.sh" "$toolchain"
hidden_version=$(sed -n 's/^Version: //p' "$hidden/MANIFEST.MF")
python3 "$root/tools/merge-bundles.py" \
    --version "$version" --r8 "$toolchain/r8.jar" --android-jar "$toolchain/android.jar" \
    --morphe-jar "$toolchain/morphe.jar" --output "$root/dist/Nesquiks-patches-$version.mpp" \
    "$scratch/x.mpp" "$root"/releases/*.mpp \
    "$hidden/Permanently-Hide-Posts-$hidden_version.mpp"
