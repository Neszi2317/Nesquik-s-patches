#!/usr/bin/env bash
# Run android/run.sh first to prepare the Robolectric cache. Use both patched APK variants.
set -euo pipefail
cd "$(dirname "$0")/../.."
tc=$(realpath "${1:?toolchain}"); cache=$(realpath "${2:?Robolectric cache}")
original=$(realpath "${3:?patched APK with original package}")
clone=$(realpath "${4:?patched APK with .morphe package}")
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/classes"
unzip -q "$tc/dex-tools.zip" -d "$work"
(cd "$work" && bash dex-tools-v2.4/d2j-dex2jar.sh -o host.jar "$clone")
# Use the shipped extension and the host's actual PreferenceInflater and performClick.
javac -source 11 -target 11 -cp "$tc/android.jar:$cache/deps/*:$work/host.jar" -d "$work/classes" \
  tests/android/SettingsRoutingTest.java
java -Drobolectric.offline=true -Drobolectric.dependency.dir="$cache/sdks" \
  -Dcom.urbandroid.ddc="$original" -Dcom.urbandroid.ddc.morphe="$clone" \
  -cp "$work/classes:$cache/deps/*:$cache/sdks/*:$work/host.jar" org.junit.runner.JUnitCore fr.nesquik.detox.SettingsRoutingTest
