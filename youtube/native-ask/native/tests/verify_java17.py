#!/usr/bin/env python3
"""Reject ANY class above Java17, including Kotlin's generated inline helpers."""
import collections,pathlib,struct,sys,zipfile
p=pathlib.Path(sys.argv[1]); counts=collections.Counter()
with zipfile.ZipFile(p) as z:
 for name in z.namelist():
  if name.endswith('.class'):
   data=z.read(name)
   assert data[:4]==b'\xca\xfe\xba\xbe',name
   major=struct.unpack('>H',data[6:8])[0];counts[major]+=1
   assert major<=61,f'{name}: class version {major}, Java17 supports up to61'
 assert counts,'No JVM classes found'
 assert 'classes.dex' in z.namelist()
 assert b'NativeProtoTest' not in z.read('extensions/ask-native.dex')
print('PASS all JVM classes <=61 (Java17), Android DEX present, no tests shipped:',dict(counts))
