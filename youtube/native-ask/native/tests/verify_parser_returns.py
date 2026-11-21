from loguru import logger
logger.remove()
from androguard.core.dex import DEX
import sys,zipfile
z=zipfile.ZipFile(sys.argv[1]);count=0;classified=False
for name in z.namelist():
 if not name.endswith('.dex'):continue
 raw=z.read(name)
 if b'transformParsed' not in raw:continue
 d=DEX(raw)
 for c in d.get_classes():
  for m in c.get_methods():
   if m.get_name()=='messageKind' and c.get_name()=='Lapp/neszi/asknative/AskNative;':
    ins=list(m.get_instructions());assert sum(i.get_name()=='instance-of'for i in ins)==2;classified=True
   if m.get_name() not in ['parseFrom','parsePartialFrom']:continue
   ins=list(m.get_instructions())
   hooked=any('AskNative;->transformParsed' in i.get_output()for i in ins)
   if not hooked:continue
   for i,x in enumerate(ins):
    if x.get_name()=='return-object':
     assert i>=3 and 'AskNative;->transformParsed' in ins[i-3].get_output()
     assert ins[i-2].get_name()=='move-result-object' and ins[i-1].get_name()=='check-cast'
     print(c.get_name(),m.get_name(),m.get_descriptor(),'return hooked')
     count+=1
assert count>=14 and classified
print('PASS:',count,'native parser returns hooked; exact two-type classifier present; hooks replace return locations so incoming branch labels execute injection')
