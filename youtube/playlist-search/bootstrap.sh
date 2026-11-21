#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TASK_TOOLS="${TASK_TOOLS:-$(pwd)/../tools}"
mkdir -p "$TASK_TOOLS"
fetch(){ [ -f "$TASK_TOOLS/$1" ] || curl --fail --location --retry 2 "$2" --output "$TASK_TOOLS/$1"; }
fetch morphe-cli.jar https://github.com/MorpheApp/morphe-desktop/releases/download/v1.18.1/morphe-desktop-1.18.1-all.jar
fetch kotlin.zip https://github.com/JetBrains/kotlin/releases/download/v2.4.21/kotlin-compiler-2.4.21.zip
fetch r8.jar https://dl.google.com/dl/android/maven2/com/android/tools/r8/8.13.17/r8-8.13.17.jar
fetch android.jar https://repo.maven.apache.org/maven2/com/google/android/android/4.1.1.4/android-4.1.1.4.jar
fetch json.jar https://repo.maven.apache.org/maven2/org/json/json/20250517/json-20250517.jar
[ -d "$TASK_TOOLS/kotlinc" ] || unzip -q "$TASK_TOOLS/kotlin.zip" -d "$TASK_TOOLS"
if [ ! -x "$TASK_TOOLS/jdk/bin/java" ]; then
 if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then ln -s "$JAVA_HOME" "$TASK_TOOLS/jdk"; else
  printf '%s\n' 'Install JDK 21 and set JAVA_HOME, or extract it under tools/jdk.' >&2
  exit 1
 fi
fi
