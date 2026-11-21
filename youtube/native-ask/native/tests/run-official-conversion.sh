#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
TC="${ASK_TOOLCHAIN:-$(cd .. && pwd)/toolchain}"
JDK="$(find "$TC" -maxdepth 1 -type d -name 'jdk-21*' | head -1)"
python3 native/tests/make_official_layout_harness.py
"$TC/protoc" --proto_path=native/research082 --java_out=lite:native/build/officialtest native/research082/feed_card_layout.proto
CP="$TC/protobuf-javalite.jar:native/build/classes:native/build/tests"
"$JDK/bin/javac" --release 11 -cp "$CP" -d native/build/officialtest/classes native/build/officialtest/app/morphe/extension/youtube/innertube/FeedCardLayoutOuterClass.java native/build/officialtest/OfficialLayoutHarness.java native/tests/OfficialConversionTest.java
"$JDK/bin/java" -cp "native/build/officialtest/classes:$CP" app.neszi.asknative.OfficialConversionTest
