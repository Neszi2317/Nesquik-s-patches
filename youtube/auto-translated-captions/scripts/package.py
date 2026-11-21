#!/usr/bin/env python3
import pathlib,sys,zipfile
root=pathlib.Path(__file__).resolve().parents[1]
def archive(path,items):
 with zipfile.ZipFile(path,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
  for name,data in sorted(items):
   info=zipfile.ZipInfo(name,(1980,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED
   info.external_attr=0o100644<<16;z.writestr(info,data)
if sys.argv[1]=='jvm':
 archive(root/'build/patch-jvm.jar',[(str(p.relative_to(root/'build/classes')),p.read_bytes()) for p in (root/'build/classes').rglob('*') if p.is_file()])
else:
 with zipfile.ZipFile(root/'build/patch-jvm.jar') as z:items=[(n,z.read(n)) for n in z.namelist()]
 items += [('classes.dex',(root/'build/dex/classes.dex').read_bytes()),('META-INF/MANIFEST.MF',(root/'MANIFEST.MF').read_bytes())]
 (root/'dist').mkdir(exist_ok=True)
 archive(root/'dist/universal-auto-translated-captions-0.4.0.mpp',items)
