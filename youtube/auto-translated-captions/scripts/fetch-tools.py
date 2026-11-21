#!/usr/bin/env python3
"""Download pinned build tools from upstream HTTPS releases. Requires JDK 21+."""
import hashlib, pathlib, urllib.request, zipfile
root=pathlib.Path(__file__).resolve().parents[1]
dest=root/'tools';dest.mkdir(exist_ok=True)
artifacts=[
 ('morphe-test.jar','https://github.com/MorpheApp/morphe-desktop/releases/download/v1.18.1/morphe-desktop-1.18.1-all.jar','1b506ab5f03d16a2f65026d5e0e1910d01fc1e2152f21eaeb44ed2f30856597b'),
 ('morphe.jar','https://github.com/MorpheApp/morphe-desktop/releases/download/v1.18.0/morphe-desktop-1.18.0-all.jar','36e20d7a18f655fb5829ae50aadd61217e2208536c0741df5f7799300f758f56'),
 ('r8.jar','https://dl.google.com/dl/android/maven2/com/android/tools/r8/8.7.18/r8-8.7.18.jar','58366f77067207c39a17d469de7b05701d2877212a9c55201bcb0af43e59e903'),
 ('kotlin.zip','https://github.com/JetBrains/kotlin/releases/download/v2.2.0/kotlin-compiler-2.2.0.zip','1adb6f1a5845ba0aa5a59e412e44c8e405236b957de1a9683619f1dca3b16932')]
for name,url,sha in artifacts:
 p=dest/name
 if not p.exists():
  print('Downloading',name,flush=True)
  with urllib.request.urlopen(url,timeout=120) as r,p.open('wb') as f:
   while chunk:=r.read(1024*1024):f.write(chunk)
 assert hashlib.sha256(p.read_bytes()).hexdigest()==sha, f'Checksum mismatch: {p}'
if not (dest/'kotlinc/bin/kotlinc').exists():
 with zipfile.ZipFile(dest/'kotlin.zip') as z:z.extractall(dest)
for p in (dest/'kotlinc/bin').iterdir():p.chmod(0o755)
print('Build tools ready; JDK 21+ must be installed separately.')
