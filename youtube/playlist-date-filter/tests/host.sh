#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
tools=$(realpath "$1")
scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT
javac --release 17 -cp "$tools/json.jar" -d "$scratch" \
    "$root/src/DateRange.java" "$root/src/PublishDate.java" "$root/tests/DateFilterTest.java"
java -cp "$scratch:$tools/json.jar" DateFilterTest
