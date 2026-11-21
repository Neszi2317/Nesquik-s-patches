#!/usr/bin/env python3
from pathlib import Path
import importlib.util,os
spec=importlib.util.spec_from_file_location('builder',Path(__file__).with_name('build.py'));b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
R,T,J,JC,K=b.ROOT,b.TOOLS,b.JAVA,b.JAVAC,b.KOTLIN
cp=lambda *p:os.pathsep.join(map(str,p))
def java(main,*paths):b.run(J,'-cp',cp(*paths),main)
for name in ['hide-nsfw','custom-home','content-filter']:
 S=R/'sources'/name;B=R/'build'/name;test=B/'tests';test.mkdir(exist_ok=True)
 if name=='hide-nsfw':
  b.run(JC,'--release','17','-cp',B/'runtime','-d',test,*list((S/'tests').rglob('*.java')));java('FilterTest',test,B/'runtime')
 elif name=='custom-home':
  b.run(JC,'--release','17','-cp',B/'runtime','-d',test,S/'tests/SubredditNameTest.java');java('SubredditNameTest',test,B/'runtime')
  host=B/'host-tests';host.mkdir(exist_ok=True)
  b.run(JC,'--release','17','-cp',cp(T/'android.jar',B/'runtime',B/'stubs'),'-d',host,*list((S/'tests/host').rglob('*.java')));java('RuntimeContractTest',host,B/'runtime',B/'stubs',T/'android.jar')
  b.run(K,'-jvm-target','17','-classpath',cp(T/'desktop.jar',B/'patch.jar'),S/'tests/SettingsHookTest.kt','-d',B/'settings-test');java('SettingsHookTestKt',T/'desktop.jar',B/'patch.jar',B/'settings-test')
 else:
  b.run(JC,'--release','8','-cp',cp(T/'android.jar',T/'json.jar',B/'runtime'),'-d',test,*list((S/'tests/com').rglob('*.java')),S/'tests/RuntimeTest.java')
  java('RuntimeTest',test,B/'runtime',T/'json.jar',T/'android-host.jar')
  b.run(JC,'-cp',T/'desktop.jar','-d',test,S/'tests/PatchOptionsTest.java');java('PatchOptionsTest',T/'desktop.jar',B/'patch.jar',test)
print('All JVM suites passed. Android UI/ART tests are not included.')
