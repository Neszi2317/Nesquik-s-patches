"""Run the production Java algorithm and menu handler with explicit platform doubles."""
from pathlib import Path
import shutil
import sys
import os
import subprocess
from tempfile import TemporaryDirectory

project = Path(__file__).resolve().parents[1]
with TemporaryDirectory() as temp:
    work = Path(temp)
    src = work / "src"
    shutil.copytree(project/"stubs", src)
    def source(package, name, declaration):
        parent = src / package.replace(".", "/")
        parent.mkdir(parents=True, exist_ok=True)
        (parent/(name+".java")).write_text("package "+package+";\n"+declaration)
    source("android.content","Context", """public class Context {
        private final SharedPreferences prefs=new SharedPreferences();
        public SharedPreferences getSharedPreferences(String n,int mode){return prefs;}
    }""")
    source("android.content","SharedPreferences", """import java.util.*;
    public class SharedPreferences {
        Set<String> ids=Set.of(); boolean enabled=true;
        final java.nio.file.Path file=java.nio.file.Path.of(System.getProperty("fixture.preferences"));
        public SharedPreferences(){
            if(!java.nio.file.Files.exists(file))return;
            try{
                List<String> lines=java.nio.file.Files.readAllLines(file);
                enabled=Boolean.parseBoolean(lines.get(0));ids=new HashSet<>(lines.subList(1,lines.size()));
            }catch(java.io.IOException e){throw new IllegalStateException(e);}
        }
        public Set<String> getStringSet(String k,Set<String> def){return ids;}
        public boolean getBoolean(String k,boolean def){return enabled;}
        public Editor edit(){return new Editor();}
        public class Editor {
            public Editor putStringSet(String k,Set<String> value){ids=new HashSet<>(value);return this;}
            public Editor putBoolean(String k,boolean v){enabled=v;return this;}
            public boolean commit(){
                try{List<String> lines=new ArrayList<>();lines.add(Boolean.toString(enabled)); lines.addAll(ids);
                    java.nio.file.Files.write(file,lines);return true;
                }catch(java.io.IOException e){return false;}
            }
        }
    }""")
    source("android.os","Looper","public class Looper {public static Looper getMainLooper(){return new Looper();}}")
    source("android.os","Handler","public class Handler {public Handler(Looper l){} public boolean post(Runnable r){r.run();return true;}}")
    source("android.widget","Toast","public class Toast {public static final int LENGTH_SHORT=0; public static Toast makeText(android.content.Context c,String s,int t){return new Toast();}public void show(){} }")
    source("app.morphe.extension.shared","Utils","public class Utils {public static final android.content.Context ctx=new android.content.Context(); public static android.content.Context getContext(){return ctx;}}")
    source("androidx.compose.runtime","w1","public class w1 {Object v; public Object getValue(){return v;} public void setValue(Object x){v=x;}}")
    source("androidx.compose.runtime","i","public class i {public static w1 s(Object x){return new w1();}}")
    source("com.x.models.timelines.items","k1","public class k1 {public com.x.models.p6 a;public com.x.models.h6 getId(){return ((com.x.models.s1)a).b.a;}}")
    source("app.morphe.extension.newx.timeline","NewXTimelineFilter", """import java.util.*;
    public class NewXTimelineFilter {
        public static class Wrapper {public Object item; public Wrapper(Object x){item=x;}}
        public static class Module {public List<?> children;public Module(List<?> c){children=c;}}
        public static boolean isTimelineModuleItem(Object x){return x instanceof Wrapper;}
        public static boolean isTimelineModule(Object x){return x instanceof Module;}
        public static Object getModuleItem(Object x){return ((Wrapper)x).item;}
        public static Object copyModuleItem(Object x,Object v,boolean d){return new Wrapper(v);}
        public static boolean isModuleItemDispensable(Object x){return true;}
        public static List<?> getModuleInnerContent(Object x){return ((Module)x).children;}
        public static Object copyModule(Object x,List<?> c,Object h,Object f,Object d,long n,String i,Object e){return new Module(c);}
        public static Object getModuleDisplayType(Object x){return null;}
        public static boolean isVerticalConversation(Object x){return false;}
        public static List<?> getVerticalConversationPostIds(Object d){return null;}
        public static Object copyVerticalConversation(Object d,List<?> ids){return d;}
        public static Object getModuleHeader(Object x){return null;}
        public static Object getModuleFooter(Object x){return null;}
        public static Object getModuleClientEventInfo(Object x){return null;}
        public static long getModuleSortIndex(Object x){return 0;}
        public static String getModuleEntryId(Object x){return null;}
        public static Object immutableList(List<?> x){return Collections.unmodifiableList(x);}
    }""")
    classes=work/"classes"; classes.mkdir()
    files=[*src.rglob("*.java"),*[p for p in (project/"runtime").glob("*.java") if p.name!="Settings.java"],project/"tests/HostTest.java",project/"tests/Restart.java"]
    subprocess.run(["java","-m","jdk.compiler/com.sun.tools.javac.Main","-d",classes,*files], check=True)
    def run(main,*args,classpath=classes):
        subprocess.run(["java","-Dfixture.preferences="+str(work/"prefs"),"-cp",classpath,main,*args],check=True,timeout=40)
    run("fr.nesquik.hidden.HostTest")
    run("fr.nesquik.hidden.Restart","seed")
    run("fr.nesquik.hidden.Restart","restore")
    if len(sys.argv)>1:
        tc=Path(sys.argv[1]).resolve()
        x=project.parents[1]/"x"
        runtime=work/"personal";runtime.mkdir()
        cp=os.pathsep.join(map(str,[tc/"json.jar",tc/"android.jar",tc/"kotlin-stdlib-2.4.jar"]))
        subprocess.run(["java","-m","jdk.compiler/com.sun.tools.javac.Main","-cp",cp,"-d",runtime,*list((x/"runtime").rglob("*.java")),*list((x/"stubs").rglob("*.java"))],check=True)
        fixtures=work/"advanced";fixtures.mkdir()
        models=fixtures/"AdvancedModels.java"
        models.write_text((x/"advanced-tests/app/morphe/extension/newx/postfilter/AdvancedModels.java").read_text().replace("class Post{","class Post extends com.x.models.timelines.items.k1{"))
        cp=os.pathsep.join(map(str,[classes,runtime,cp]))
        subprocess.run(["java","-m","jdk.compiler/com.sun.tools.javac.Main","-cp",cp,"-d",fixtures,models,project/"tests/FilterCombination.java"],check=True)
        run("app.morphe.extension.newx.postfilter.FilterCombination",classpath=str(fixtures)+os.pathsep+cp)
