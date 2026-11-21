#!/usr/bin/env python3
"""Apply 41 default official patches + the 3 custom patches and inspect DEX."""
from pathlib import Path
import importlib.util,os,subprocess,sys,json
spec=importlib.util.spec_from_file_location('builder',Path(__file__).with_name('build.py'));b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
R,T,J,JC=b.ROOT,b.TOOLS,b.JAVA,b.JAVAC
if len(sys.argv)!=2:raise SystemExit('Usage: python3 integration.py /path/to/original-2026.47.0.apkm')
out=R/'validation';out.mkdir(exist_ok=True)
official=T/'official.mpp'
if not official.exists():official=T/'patches-2.4.0-dev.6.mpp'
cmd=[J,'-Xmx4g','-jar',T/'desktop.jar','patch',Path(sys.argv[1]).resolve(),'-p',official,'-p',R/'patches/reddit-custom-home-0.2.0.mpp','-e','Custom Home & Disable Main Feed','-p',R/'patches/reddit-hide-nsfw-1.1.0.mpp','-e','Hide NSFW posts','-p',R/'patches/RedditContentFilter-v2.3.mpp','-e','Reddit - Local keyword filters','--unsigned','--validate','-o',out/'combined.apk','-r',out/'combined.json']
with (out/'combined.log').open('w') as f:subprocess.run(list(map(str,cmd)),check=True,stdout=f,stderr=subprocess.STDOUT)
result=json.loads((out/'combined.json').read_text())
if not result.get('success') or result.get('failedPatches'):raise SystemExit('Patching failure: '+str(result))
classes=R/'build/analysis';classes.mkdir(exist_ok=True)
b.run(JC,'-cp',T/'desktop.jar','-d',classes,R/'analysis/VerifyIntegration.java')
b.run(J,'-Xmx4g','-cp',os.pathsep.join(map(str,[T/'desktop.jar',classes])),'VerifyIntegration',out/'combined.apk')
print('PASS:',len(result['appliedPatches']),'patches applied. Output is unsigned; not an installation deliverable.')
