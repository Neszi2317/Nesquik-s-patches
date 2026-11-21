#!/usr/bin/env python3
from pathlib import Path
import json,urllib.request,hashlib,zipfile,tarfile,os
root=Path(__file__).resolve().parent;tools=Path(os.environ.get('TOOLS_DIR',root/'tools')).resolve();tools.mkdir(exist_ok=True)
for name,item in json.loads((root/'toolchain.json').read_text()).items():
 p=tools/name
 if not p.exists() or hashlib.sha256(p.read_bytes()).hexdigest()!=item['sha256']:urllib.request.urlretrieve(item['url'],p)
 if hashlib.sha256(p.read_bytes()).hexdigest()!=item['sha256']:raise SystemExit('Checksum mismatch: '+name)
if not (tools/'kotlinc').exists():
 with zipfile.ZipFile(tools/'kotlin.zip') as z:z.extractall(tools)
if not (tools/'jdk21').exists():
 (tools/'jdk21').mkdir()
 with tarfile.open(tools/'jdk21.tar.gz') as tf:
  prefix=tf.getmembers()[0].name.split('/')[0]+'/'
  for m in tf.getmembers():
   if m.name.startswith(prefix) and m.name!=prefix:m.name=m.name[len(prefix):];tf.extract(m,tools/'jdk21',filter='data')
for d in [tools/'kotlinc/bin',tools/'jdk21/bin']:
 for p in d.iterdir():p.chmod(p.stat().st_mode|0o111)
print('Verified toolchain:',tools)
