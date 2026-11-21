#!/usr/bin/env python3
"""Public reproducible dependencies; no GitHub credentials required."""
import concurrent.futures, pathlib, tarfile, urllib.request
root=pathlib.Path(__file__).resolve().parents[2]
tc=root/'toolchain';tc.mkdir(exist_ok=True)
urls={
 'protoc':'https://repo.maven.apache.org/maven2/com/google/protobuf/protoc/3.25.5/protoc-3.25.5-linux-x86_64.exe',
 'protobuf-javalite.jar':'https://repo.maven.apache.org/maven2/com/google/protobuf/protobuf-javalite/3.25.5/protobuf-javalite-3.25.5.jar',
 'desktop.jar':'https://github.com/MorpheApp/morphe-desktop/releases/download/v2.1.0-dev.1/morphe-desktop-2.1.0-dev.1-all.jar',
 'kotlin.jar':'https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-compiler-embeddable/2.4.10/kotlin-compiler-embeddable-2.4.10.jar',
 'android.jar':'https://raw.githubusercontent.com/Sable/android-platforms/master/android-35/android.jar',
 'r8.jar':'https://dl.google.com/dl/android/maven2/com/android/tools/r8/9.1.31/r8-9.1.31.jar',
 'annotations.jar':'https://repo.maven.apache.org/maven2/org/jetbrains/annotations/26.0.2/annotations-26.0.2.jar',
 'jdk.tar.gz':'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/OpenJDK21U-jdk_x64_linux_hotspot_21.0.13_9.tar.gz',
 'official-current.mpp':'https://github.com/MorpheApp/morphe-patches/releases/download/v2.1.0-dev.8/patches-2.1.0-dev.8.mpp',
}
def get(item):
 name,url=item;dest=tc/name
 if not dest.exists():urllib.request.urlretrieve(url,dest)
 print(name,dest.stat().st_size,flush=True)
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:list(pool.map(get,urls.items()))
if not list(tc.glob('jdk-21*/bin/java')):
 with tarfile.open(tc/'jdk.tar.gz') as archive:archive.extractall(tc,filter='data')
(tc/'protoc').chmod(0o755)
import shutil
shutil.copyfile(tc/'desktop.jar',tc/'desktop-current.jar')
print('Run bash native/build.sh from the restored project.')
