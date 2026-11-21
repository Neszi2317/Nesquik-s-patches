#!/usr/bin/env python3
"""Interpret the two ACTUAL generated DEX helper instruction streams.

Input is Inspect.java's dump of the patched APK. Native YouTube builders,
Locale labels, and originalCaptionMenu are mocked. This is NOT Android or a
YouTube network test. Unsupported opcodes/methods fail immediately.
ABI mock names below are verified for YouTube 21.16.256 only.
"""
import re, sys
from pathlib import Path
from urllib.parse import parse_qs, urlsplit

text=Path(sys.argv[1]).read_text()
methods={}
for block in re.split(r'(?=^L[^\n]+ flags=)',text,flags=re.M):
    lines=block.splitlines()
    if not lines or '->morphe_' not in lines[0]: continue
    name=lines[0].split('->',1)[1].split('(',1)[0]
    if name not in ('morphe_addCaptionTarget','morphe_completeTranslations'): continue
    regs=int(lines[1].split('=')[1]); ins={}
    for line in lines[2:]:
        match=re.match(r'(\d+) ([A-Z0-9_]+)(.*)',line)
        if not match: continue
        pc,op,rest=match.groups(); data={k:int(v) for k,v in re.findall(r'(A|B|C|offset|literal|count)=(-?\d+)',rest)}
        if ' ref=' in rest:data['ref']=rest.split(' ref=',1)[1]
        if 'regs=' in rest:data['regs']=[int(x) for x in re.search(r'regs=\[([^]]+)\]',rest)[1].split(',')][:data['count']]
        ins[int(pc)]=(op,data)
    methods[name]=(regs,ins)
assert len(methods)==2

# Obfuscated fields are used in mocks only, never in the patch source.
LANG='Laosh;->a:Ljava/lang/String;'
URL='Laosh;->m:Ljava/lang/String;'
VIDEO='Laosh;->d:Ljava/lang/String;'
VSS='Laosh;->k:Ljava/lang/String;'
KIND='Laosh;->l:Ljava/lang/String;'
OFFLINE='Laosj;->e:Z'
META='Laosj;->a:Lbfdp;'
TARGETS='Lbfdp;->d:Lawoi;'
TARGET='Lbfdq;->c:Ljava/lang/String;'

def track(lang,kind='',url=None):
    return {LANG:lang,URL:url if url is not None else f'https://www.youtube.com/api/timedtext?v=test&lang={lang}',VIDEO:'test',VSS:'.'+lang,KIND:kind}
class Iter:
    def __init__(self,items):self.items=items;self.i=0
    def next(self):a=self.items[self.i];self.i+=1;return a

def call(ref,args,legacy):
    method=ref.split('->')[1].split('(')[0]
    if '->morphe_originalCaptionMenu(' in ref:return args[0]['sources']
    if '->morphe_addCaptionTarget(' in ref:return run('morphe_addCaptionTarget',args,legacy)
    if ref.startswith('Ljava/lang/String;'):
        s=args[0]
        if method=='isEmpty':return len(s)==0
        if method=='equalsIgnoreCase':return isinstance(args[1],str) and s.lower()==args[1].lower()
        if method=='equals':return s==args[1]
        if method=='concat':return s+args[1]
        if method=='startsWith':return s.startswith(args[1])
        if method=='contains':return args[1] in s
        if method=='split':return s.split(args[1])
    if ref.startswith('Ljava/util/List;'):
        if method=='iterator':return Iter(args[0])
        if method=='isEmpty':return len(args[0])==0
        if method=='add':args[0].append(args[1]);return True
    if ref.startswith('Ljava/util/Iterator;'):
        if method=='hasNext':return args[0].i<len(args[0].items)
        if method=='next':return args[0].next()
    if ref.startswith('Ljava/util/Locale;'):return args[0] # Label rendering is not tested.
    if ref=='Laosh;->a()Laosg;':return {}
    if ref=='Laosh;->d(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;':return 't'+args[0]+'.'+args[1]
    if ref.startswith('Laosg;'):
        if method=='a':return args[0].copy()
        field={'i':LANG,'e':'id','n':VIDEO,'o':VSS,'m':URL}[method]
        args[0][field]=args[1];return None
    raise AssertionError(('Unmocked invocation',ref))

def run(name,args,legacy=False):
    size,ins=methods[name];r=[None]*size;r[size-len(args):]=args
    addresses=list(ins);following=dict(zip(addresses,addresses[1:]+[-1]));pc=0;result=None;steps=0
    while True:
        steps+=1;assert steps<200000
        op,d=ins[pc];a=d.get('A');b=d.get('B');nxt=following[pc]
        if op.startswith('INVOKE_'):result=call(d['ref'],[r[x] for x in d['regs']],legacy)
        elif op in ('MOVE_RESULT','MOVE_RESULT_OBJECT'):r[a]=result
        elif op=='MOVE_OBJECT':r[a]=r[b]
        elif op in ('IGET_BOOLEAN','IGET_OBJECT'):r[a]=r[b][d['ref']]
        elif op=='IPUT_OBJECT':r[b][d['ref']]=r[a]
        elif op=='CHECK_CAST':pass
        elif op=='CONST_STRING':r[a]=d['ref']
        elif op=='CONST_4':r[a]=d['literal']
        elif op=='ARRAY_LENGTH':r[a]=len(r[b])
        elif op=='AGET_OBJECT':r[a]=r[b][r[d['C']]]
        elif op=='ADD_INT_LIT8':r[a]=r[b]+d['literal']
        elif op in ('IF_EQZ','IF_NEZ','IF_GE','GOTO'):
            zero = r[a] is None or r[a] == 0 if a is not None else False
            jump=op=='GOTO' or op=='IF_EQZ' and zero or op=='IF_NEZ' and not zero or op=='IF_GE' and r[a]>=r[b]
            if jump:
                nxt=pc+d['offset']
                # Reproduce the verified 0.3.0 server-loop exit for regression.
                if legacy and name=='morphe_completeTranslations' and pc==32:nxt=66
        elif op=='RETURN_VOID':return
        else:raise AssertionError(('Unsupported opcode',op))
        pc=nxt

def complete(sources,server,native=None,offline=False,legacy=False):
    owner={'sources':sources,OFFLINE:offline,META:{TARGETS:[{TARGET:x} for x in server]}}
    out=[] if native is None else native.copy()
    run('morphe_completeTranslations',[owner,out],legacy)
    return out

checks=0
def check(ok,label):
    global checks
    assert ok,label
    checks+=1;print('PASS',label)

# Demonstrate the old failure and the corrected path using the same inputs.
old=complete([track('fr')],['en'],legacy=True)
new=complete([track('fr')],['en'])
check('bn' not in [t[LANG] for t in old] and 'bn' in [t[LANG] for t in new], 'partial server list: 0.3.0 misses Bengali; 0.4.0 adds it')
for src,target in [('fr','bn'),('ar','fr'),('fr','ar'),('en','bn')]:
    out=complete([track(src)],['en','en','AR'])
    t=next(t for t in out if t[LANG].lower()==target)
    query=parse_qs(urlsplit(t[URL]).query)
    check(query['lang']==[src] and query['tlang'][0].lower()==target,f'{src} -> {target}: generated track preserves source URL and target')
    check(len(out)==len({t[LANG].lower() for t in out}),f'{src}: no duplicate language codes')
    check(src not in {t[LANG] for t in out},f'{src}: no redundant translation into source language')
check(len(new)==len(complete([track('fr')],[])), 'empty and partial server lists both reach full fallback catalogue')
existing=track('bn',url='https://www.youtube.com/api/timedtext?v=test&lang=fr&tlang=bn')
out=complete([track('fr')],['bn'],[existing])
check(sum(x[LANG]=='bn' for x in out)==1 and out[0] is existing,'existing native Bengali track preserved unchanged')
check(complete([],['bn'])==[],'no source captions: no manufactured tracks')
check(complete([track('fr')],['bn'],offline=True)==[],'offline guard preserved')
for url in ['', 'https://www.youtube.com/api/timedtext','https://www.youtube.com/api/timedtext?lang=fr&tlang=en']:
    check(complete([track('fr',url=url)],[])==[],'invalid or already translated source URL skipped: '+url)
manual=track('fr');asr=track('en','asr')
out=complete([asr,manual],['bn'])
check(parse_qs(urlsplit(next(t for t in out if t[LANG]=='bn')[URL]).query)['lang']==['fr'],'manual source preferred over ASR, as in 0.3.0')
check('zz-server' in {t[LANG] for t in complete([track('fr')],['zz-server'])},'server additions outside fallback catalogue are retained (synthetic code)')
print(f'{checks} checks passed; mocked instruction execution only, no device/network translation validation.')
