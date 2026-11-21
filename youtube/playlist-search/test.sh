#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TASK_TOOLS="${TASK_TOOLS:-$(cd ../tools && pwd)}"
mkdir -p build/tests
"$TASK_TOOLS/jdk/bin/javac" --release 8 -Xlint:-options -cp "$TASK_TOOLS/json.jar" -d build/tests src/main/java/app/nesquikos/playlist/PlaylistIndex.java src/main/java/app/nesquikos/playlist/PlaylistSession.java tests/IndexTest.java tests/PageProbe.java
"$TASK_TOOLS/jdk/bin/java" -cp "$TASK_TOOLS/json.jar:build/tests" IndexTest "$@"
