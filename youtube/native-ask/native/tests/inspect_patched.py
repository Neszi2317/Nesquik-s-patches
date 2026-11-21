from loguru import logger
logger.remove()
from androguard.core.dex import DEX
import zipfile,sys
z=zipfile.ZipFile(sys.argv[1])
for f in z.namelist():
 if not f.endswith('.dex'):continue
 raw=z.read(f)
 if b'Lapp/neszi/asknative/AskNative;' not in raw:continue
 d=DEX(raw)
 for c in d.get_classes():
  for m in c.get_methods():
   if (c.get_name().endswith('/AskFlyoutMenuPatch;') and m.get_name()=='showAskPanel') or any('Lapp/neszi/asknative/AskNative;' in i.get_output() for i in m.get_instructions()):
    print(f,c.get_name(),m.get_name(),m.get_descriptor(), 'REGISTERS',m.get_code().get_registers_size() if m.get_code() else 0)
    for i in m.get_instructions(): print(i.get_name(),i.get_output())
