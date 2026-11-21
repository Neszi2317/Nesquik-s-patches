#!/usr/bin/env python3
"""Build all three MPP bundles. Run fetch-tools.py first, or set TOOLS_DIR."""
from pathlib import Path
import os,subprocess,shutil,zipfile
ROOT=Path(__file__).resolve().parent
TOOLS=Path(os.environ.get('TOOLS_DIR',ROOT/'tools')).resolve()
JAVA=TOOLS/'jdk21/bin/java';JAVAC=TOOLS/'jdk21/bin/javac';JAR=TOOLS/'jdk21/bin/jar';KOTLIN=TOOLS/'kotlinc/bin/kotlinc'
def run(*args):subprocess.run([str(a) for a in args],check=True)
def build(name,version,output,extension):
 src=ROOT/'sources'/name;b=ROOT/'build'/name
 if b.exists():shutil.rmtree(b)
 for d in ['stubs','runtime','extension','patch','patch-dex']:(b/d).mkdir(parents=True)
 stubfiles=list((src/'stubs').rglob('*.java'))
 if stubfiles:run(JAVAC,'--release','17','-cp',TOOLS/'android.jar','-d',b/'stubs',*stubfiles)
 files=list((src/('runtime' if name=='content-filter' else 'src/main/java')).rglob('*.java'))
 cp=os.pathsep.join(map(str,[TOOLS/'android.jar',TOOLS/'json.jar',b/'stubs']))
 run(JAVAC,'--release','8' if name=='content-filter' else '17','-cp',cp,'-d',b/'runtime',*files)
 run(JAR,'cf',b/'runtime.jar','-C',b/'runtime','.')
 run(JAVA,'-cp',TOOLS/'r8.jar','com.android.tools.r8.D8','--min-api','26' if name=='content-filter' else '28','--lib',TOOLS/'android.jar','--lib',TOOLS/'jdk21','--classpath',b/'stubs','--classpath',TOOLS/'json.jar','--output',b/'extension',b/'runtime.jar')
 ks=list((src/('src' if name=='content-filter' else 'src/main/kotlin')).rglob('*.kt'))
 extra=['-Xlambdas=class'] if name=='content-filter' else []
 run(KOTLIN,'-jvm-target','11' if name=='content-filter' else '17',*extra,'-classpath',TOOLS/'desktop.jar',*ks,'-d',b/'patch.jar')
 run(JAVA,'-cp',TOOLS/'r8.jar','com.android.tools.r8.D8','--min-api','26','--lib',TOOLS/'android.jar','--lib',TOOLS/'jdk21','--classpath',TOOLS/'desktop.jar','--output',b/'patch-dex',b/'patch.jar')
 out=ROOT/'patches'/output;out.parent.mkdir(exist_ok=True)
 manifest='\r\n'.join(['Manifest-Version: 1.0','Name: '+name,'Version: '+version,'Patcher-Version: 2.0.0','Description: Reddit 2026.47.0 compatibility port','',''])
 with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
  z.writestr('META-INF/MANIFEST.MF',manifest)
  with zipfile.ZipFile(b/'patch.jar') as jar:
   for n in jar.namelist():
    if n!='META-INF/MANIFEST.MF' and not n.endswith('/'):z.writestr(n,jar.read(n))
  z.write(b/'patch-dex/classes.dex','classes.dex');z.write(b/'extension/classes.dex',extension)
  for license in ['LICENSE','NOTICE','NOTICE.md']:
   if (src/license).exists():z.write(src/license,license)
 print('BUILT',out)
if __name__=='__main__':
 build('content-filter','2.3.0','RedditContentFilter-v2.3.mpp','extensions/reddit-keywords.dex')
 build('hide-nsfw','1.1.0-experimental','reddit-hide-nsfw-1.1.0.mpp','nsfw-extension.dex')
 build('custom-home','0.2.0-experimental','reddit-custom-home-0.2.0.mpp','custom-home-extension.dex')
