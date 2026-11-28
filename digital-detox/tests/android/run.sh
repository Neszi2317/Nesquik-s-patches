#!/usr/bin/env bash
set -euo pipefail
# JDK 21, Maven, Android compile SDK from the toolchain, curl and unzip.
cd "$(dirname "$0")/../.."
tc=$(realpath "${1:?toolchain directory}")
cache=$(realpath -m "${2:-build/android-test}")
mkdir -p "$cache"/{deps,sdks,classes}
mvn -B -ntp -f tests/android/pom.xml org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies -DoutputDirectory="$cache/deps"
for aar in "$cache"/deps/*.aar;do unzip -p "$aar" classes.jar > "${aar%.aar}.jar";done
version=16-robolectric-13921718-i7
sdk="android-all-instrumented-$version.jar"
if [[ ! -s "$cache/sdks/$sdk" ]];then
    curl -fLsS "https://repo.maven.apache.org/maven2/org/robolectric/android-all-instrumented/$version/$sdk" -o "$cache/$sdk.tmp"
    mv "$cache/$sdk.tmp" "$cache/sdks/$sdk"
fi
javac -source 11 -target 11 -cp "$tc/android.jar:$cache/deps/*" -d "$cache/classes" $(find stubs runtime -name '*.java') tests/android/SessionTest.java
java -Drobolectric.offline=true -Drobolectric.dependency.dir="$cache/sdks" -cp "$cache/classes:$cache/deps/*:$cache/sdks/*" org.junit.runner.JUnitCore fr.nesquik.detox.SessionTest
