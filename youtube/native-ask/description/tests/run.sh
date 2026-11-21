#!/usr/bin/env bash
set -euo pipefail
ND="$(cd "$(dirname "$0")/.." && pwd)"
TC="${ASK_TOOLCHAIN:-$(cd "$ND/../.." && pwd)/toolchain}"
JDK="$(find "$TC" -maxdepth 1 -type d -name 'jdk-21*' | head -1)"
mkdir -p "$ND/build/tests"
"$JDK/bin/javac" --release 11 -cp "$ND/build/classes" -d "$ND/build/tests" $(find "$ND/tests/stubs" -name '*.java') "$ND/tests/DescriptionTest.java"
"$JDK/bin/java" -cp "$ND/build/tests:$ND/build/classes" app.neszi.askdescription.DescriptionTest
