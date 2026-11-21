import java.io.File;
import java.util.*;
import java.util.stream.StreamSupport;
import com.android.apksig.ApkVerifier;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

public final class InspectApk {
    static final String PREFIX="Lfr/nesquik/hidden/";
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);System.out.println(text);}
    public static void main(String[] args)throws Exception{
        File apk=new File(args[0]);
        var signature=new ApkVerifier.Builder(apk).setMinCheckedPlatformVersion(28).build().verify();
        check(signature.isVerified(),"APK signature verified");
        var container=DexFileFactory.loadDexContainer(apk,Opcodes.getDefault());
        Map<String,ClassDef> types=new HashMap<>();
        for(String entry:container.getDexEntryNames()){
            for(ClassDef c:container.getEntry(entry).getDexFile().getClasses()){
                if(types.put(c.getType(),c)!=null)throw new AssertionError("Duplicate class "+c);
            }
        }
        check(types.containsKey(PREFIX+"Settings;"),types.size()+" unique DEX classes including Settings");
        Map<String,Integer> links=new TreeMap<>();
        Set<String> watched=Set.of("Landroidx/compose/material/d0;","Lcom/x/models/x5;",
            "Lcom/x/urt/items/post/options/j0;","Lcom/x/urt/items/post/options/o;",
            "Lcom/x/urt/ui/b;","Lcom/x/postdetailsheet/r;","Lcom/x/postdetailsheet/k;",
            "Lcom/x/video/tab/g9;","Lcom/x/video/tab/k7;","Lcom/x/video/tab/z7;","Lcom/x/urt/ui/t0;",
            "Lcom/x/urt/ui/module/s;","Lcom/google/android/gms/internal/mlkit_vision_common/d9;",
            "Lapp/morphe/extension/crimera/settings/SettingsRegistry;",
            "Lapp/morphe/extension/newx/misc/NewXObjectBrowserHandler;",
            "Lapp/morphe/extension/newx/timeline/NewXTimelineFilter;");
        for(ClassDef c:types.values()){
            if(!c.getType().startsWith(PREFIX) && !watched.contains(c.getType()))continue;
            for(Method m:c.getMethods()){
            if(m.getImplementation()==null)continue;
            for(Instruction ins:m.getImplementation().getInstructions()){
                if(!(ins instanceof ReferenceInstruction ri))continue;
                Reference ref=ri.getReference();
                if(ref instanceof MethodReference target){
                    String owner=target.getDefiningClass(),name=target.getName();
                    if(owner.startsWith(PREFIX) && !c.getType().startsWith(PREFIX)){
                        links.merge(owner+"->"+name,1,Integer::sum);
                    }
                    if(c.getType().startsWith(PREFIX) && (owner.startsWith("Lcom/x/") || owner.startsWith("Lapp/morphe/") || owner.startsWith("Landroidx/compose/runtime/"))){
                        ClassDef impl=types.get(owner);
                        boolean publicMethod=impl!=null && StreamSupport.stream(impl.getMethods().spliterator(),false)
                            .anyMatch(t->t.toString().equals(target.toString()) && AccessFlags.PUBLIC.isSet(t.getAccessFlags()));
                        if(!publicMethod && impl!=null && "Landroid/app/Fragment;".equals(impl.getSuperclass())){
                            String descriptor=target.toString().split("->",2)[1];
                            publicMethod=Arrays.stream(Class.forName("android.app.Fragment",false,InspectApk.class.getClassLoader()).getMethods())
                                .anyMatch(t->(t.getName()+java.lang.invoke.MethodType.methodType(t.getReturnType(),t.getParameterTypes()).descriptorString()).equals(descriptor));
                        }
                        if(!publicMethod)throw new AssertionError(target);
                    }
                    if(c.getType().endsWith("/NewXTimelineFilter;") && (owner.endsWith("/AdvancedFilter;") || owner.endsWith("/SensitivePostFilter;")) && name.equals("filter"))links.put(owner,1);
                }
                if(ref instanceof FieldReference f && c.getType().startsWith(PREFIX) && f.getDefiningClass().startsWith("Lcom/x/")){
                    if(StreamSupport.stream(types.get(f.getDefiningClass()).getFields().spliterator(),false)
                        .noneMatch(t->t.toString().equals(f.toString()) && AccessFlags.PUBLIC.isSet(t.getAccessFlags())))throw new AssertionError(f);
                }
                if(ref instanceof StringReference s && s.getString().equals("Lfr/nesquik/hidden/Settings;") && c.getType().endsWith("/SettingsRegistry;"))links.put("settings registry",1);
                if(ref instanceof FieldReference f && f.getDefiningClass().equals(PREFIX+"Menu;") && f.getName().equals("action") && c.getType().equals("Lcom/x/models/x5;") && ins.getOpcode()==Opcode.SPUT_OBJECT)links.put("private menu token initialized",1);
                if(ref instanceof MethodReference f && f.getDefiningClass().endsWith("/NewXObjectBrowserHandler;") && !c.getType().startsWith("Lapp/morphe/"))links.put("native menu "+f.getName(),1);
            }
        }
        }
        for(var e:links.entrySet())System.out.println(e.getKey()+" = "+e.getValue());
        check(links.getOrDefault(PREFIX+"Projection;->render",0)>=9,"UI list hooks present");
        for(String name:List.of("add","handle","icon","label"))
            check(links.containsKey(PREFIX+"Menu;->"+name),"Extension menu hook "+name);
        for(String name:List.of("addOption","handleOptionAction","labelFor","usesIcon"))
            check(links.containsKey("native menu "+name),"Native dispatch "+name);
        check(links.containsKey("settings registry") && links.containsKey("private menu token initialized"),"Settings and menu token linked");
        if(args.length>1)check(links.keySet().stream().anyMatch(k->k.endsWith("/AdvancedFilter;")) && links.keySet().stream().anyMatch(k->k.endsWith("/SensitivePostFilter;")),"Personal keyword and sensitive filters linked");
    }
}
