import java.io.*;import java.util.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.reference.*;
public class VerifyDex {
 public static void main(String[] args)throws Exception{
  var container=DexFileFactory.loadDexContainer(new File(args[0]),Opcodes.getDefault());Map<String,ClassDef> classes=new HashMap<>();
  for(String n:container.getDexEntryNames())for(ClassDef c:container.getEntry(n).getDexFile().getClasses())if(classes.put(c.getType(),c)!=null)throw new AssertionError("Duplicate class "+c.getType());
  Set<String> runtime=new HashSet<>();int count=0,calls=0;Map<String,Integer> hookCounts=new TreeMap<>();
  for(ClassDef c:classes.values())if(c.getType().startsWith("Llocal/reddit/filter/")){count++;for(Method m:c.getMethods())runtime.add(m.toString());}
  if(count<5)throw new AssertionError("Runtime extension missing");
  for(ClassDef c:classes.values())for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction i:m.getImplementation().getInstructions()){
   if(i instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference ref && ref.getDefiningClass().startsWith("Llocal/reddit/filter/")){
    if(!c.getType().startsWith("Llocal/reddit/filter/")&&!runtime.contains(ref.toString()))throw new AssertionError("Missing injected runtime method "+ref);
    if(!c.getType().startsWith("Llocal/reddit/filter/")){calls++;hookCounts.merge(ref.getName(),1,Integer::sum);System.out.println("HOOK "+m.getDefiningClass()+"->"+m.getName()+" -> "+ref.getName());}
   }
   if(c.getType().startsWith("Llocal/reddit/filter/")&&i instanceof ReferenceInstruction r&&r.getReference().toString().startsWith("Ljava/net/"))throw new AssertionError("Network reference in local runtime");
  }
  Map<String,Integer> expected=Map.of("filterFeedElements",1,"filterListing",1,"filterCommentState",2,"hideFeedPost",1,"rememberLink",3,"rememberGraphqlPost",1,"initialize",1,args.length>1&&args[1].equals("standalone")?"openSettings":"addSettingsFragment",1,"filterSearchItems",2);
  if(!hookCounts.equals(expected))throw new AssertionError("Unexpected hook counts: "+hookCounts);
  ClassDef config=classes.get("Llocal/reddit/filter/BundledConfig;");if(config==null)throw new AssertionError("Bundled config missing");
  Map<String,String> defaults=new TreeMap<>();
  for(Method m:config.getMethods())if(m.getName().equals("keywords")||m.getName().equals("subreddits")){
   StringBuilder text=new StringBuilder();for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference str)text.append(str.getString());
   defaults.put(m.getName(),text.toString());
  }
  if(args.length>1&&args[1].equals("standalone")){
   if(!defaults.equals(Map.of("keywords","","subreddits","")))throw new AssertionError("Standalone defaults unexpectedly nonempty");
  }else {
   if(!defaults.get("keywords").equals("second keyword\ncardiology\nexampleblockedword\nliteral.a+(b)[c]"))throw new AssertionError("Keyword options not embedded exactly: "+defaults);
   if(!defaults.get("subreddits").equals("askreddit\naskhistorians\naskscience"))throw new AssertionError("Community options not embedded exactly: "+defaults);
  }
  System.out.println("PASS Manager list + TXT configuration normalized and embedded in DEX");
  System.out.println("PASS runtime_classes="+count+" injected_calls="+calls+" all targets resolved; no runtime networking references.");
  System.out.println("Static DEX checks only; not Android ART execution.");
 }
}
