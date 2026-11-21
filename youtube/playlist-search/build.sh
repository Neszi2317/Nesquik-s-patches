#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
TASK_TOOLS="${TASK_TOOLS:-$(cd ../tools && pwd)}"
export JAVA_HOME="$TASK_TOOLS/jdk"
export PATH="$JAVA_HOME/bin:$PATH"
mkdir -p build/java build/extension-dex build/patch-dex dist
javac --release 8 -Xlint:-options -cp "$TASK_TOOLS/android.jar:$TASK_TOOLS/json.jar" -d build/java $(find src/main/java -name '*.java')
jar cf build/extension.jar -C build/java .
java -cp "$TASK_TOOLS/r8.jar" com.android.tools.r8.D8 --release --min-api 28 --lib "$TASK_TOOLS/android.jar" --lib "$JAVA_HOME" --output build/extension-dex build/extension.jar
"$TASK_TOOLS/kotlinc/bin/kotlinc" -language-version 2.2 -api-version 2.2 -Xcontext-parameters -jvm-target 17 -cp "$TASK_TOOLS/morphe-cli.jar" src/main/kotlin -d build/patch.jar
java -cp "$TASK_TOOLS/r8.jar" com.android.tools.r8.D8 --release --min-api 26 --lib "$TASK_TOOLS/android.jar" --lib "$JAVA_HOME" --classpath "$TASK_TOOLS/morphe-cli.jar" --output build/patch-dex build/patch.jar
python3 - <<'PY'
import zipfile
with zipfile.ZipFile('build/patch.jar') as src,zipfile.ZipFile('dist/YouTube-Search-in-Playlists.mpp','w',zipfile.ZIP_DEFLATED) as dst:
 for n in src.namelist():
  if n!='META-INF/MANIFEST.MF':dst.writestr(n,src.read(n))
 dst.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\r\nName: YouTube Search in Playlists (unofficial)\r\nVersion: 0.1.0-experimental\r\nPatcher-Version: 1.15.1\r\nAuthor: Nesquikos custom patches\r\nLicense: GPL-3.0\r\n\r\n')
 dst.write('build/patch-dex/classes.dex','classes.dex');dst.write('build/extension-dex/classes.dex','playlist-extension.dex')
 dst.write('LICENSE','LICENSE');dst.write('NOTICE','NOTICE')
PY
java -jar "$TASK_TOOLS/morphe-cli.jar" list-patches --patches dist/YouTube-Search-in-Playlists.mpp
