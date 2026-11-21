import java.io.*;import java.util.*;import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.reference.*;
public class VerifyIntegration {
 static Map<String,ClassDef> cs=new HashMap<>();static int checks;
 static boolean custom(String t){return t.startsWith("Llocal/reddit/filter/")||t.startsWith("Lapp/neszi/reddit/nsfw/")||t.startsWith("Lapp/neszi/reddit/customhome/");}
 static void ok(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static boolean hasMethod(MethodReference r,String t,Set<String> seen){if(!seen.add(t))return false;var c=cs.get(t);if(c==null){ if(t.startsWith("Landroid/"))return true; if(t.startsWith("Ljava/")){ try {Class<?> k=Class.forName(t.substring(1,t.length()-1).replace('/','.'));for(var x:k.getMethods())if(x.getName().equals(r.getName()) && x.getParameterCount()==r.getParameterTypes().size())return true;}catch(Exception ignored){} }return false;}for(Method m:c.getMethods())if(m.getName().equals(r.getName())&&m.getParameterTypes().equals(r.getParameterTypes())&&m.getReturnType().equals(r.getReturnType()))return true;if(c.getSuperclass()!=null&&hasMethod(r,c.getSuperclass(),seen))return true;for(String it:c.getInterfaces())if(hasMethod(r,it,seen))return true;return false;}
 static boolean hasField(FieldReference r,String t){var c=cs.get(t);if(c==null)return false;for(Field f:c.getFields())if(f.getName().equals(r.getName())&&f.getType().equals(r.getType()))return true;return c.getSuperclass()!=null&&hasField(r,c.getSuperclass());}
 public static void main(String[] args)throws Exception {var d=DexFileFactory.loadDexContainer(new File(args[0]),Opcodes.getDefault());for(String n:d.getDexEntryNames())for(ClassDef c:d.getEntry(n).getDexFile().getClasses())ok(cs.put(c.getType(),c)==null,"duplicate class "+c.getType());
 int refs=0,bridges=0,customClasses=0;Map<String,Integer> hooks=new TreeMap<>();
 for(ClassDef c:cs.values()){
  boolean own=custom(c.getType());if(own)customClasses++;
  Set<String> unique=new HashSet<>();for(Method m:c.getMethods()){ok(unique.add(m.getName()+m.getParameterTypes()+m.getReturnType()),"duplicate method "+m);if(m.getName().equals("neszi_nsfwSources"))bridges++;
   boolean injected=own||m.getName().startsWith("neszi_");if(m.getImplementation()==null)continue;
   for(Instruction i:m.getImplementation().getInstructions()){
    if(injected&&i instanceof OneRegisterInstruction x)ok(x.getRegisterA()<m.getImplementation().getRegisterCount(),"register A overflow "+m);
    if(!(i instanceof ReferenceInstruction ri))continue;var r=ri.getReference();
    if(r instanceof MethodReference mr&&(injected||custom(mr.getDefiningClass()))){
     String t=mr.getDefiningClass();if(cs.containsKey(t)){ok(hasMethod(mr,t,new HashSet<>()),"unresolved method "+m+" -> "+mr);refs++;}
     if(!own&&custom(t))hooks.merge(mr.getName(),1,Integer::sum);
     if(own)ok((!t.startsWith("Ljava/net/")||t.equals("Ljava/net/URI;")||t.equals("Ljava/net/URISyntaxException;"))&&!t.startsWith("Lokhttp3/"),"network reference "+mr);
    }
    if(injected&&r instanceof FieldReference fr&&cs.containsKey(fr.getDefiningClass())){ok(hasField(fr,fr.getDefiningClass()),"unresolved field "+m+" -> "+fr);refs++;}
   }
  }
 }
 var settings=cs.get("Lapp/morphe/extension/reddit/settings/preference/RedditPreferenceFragment;");ok(settings!=null,"Morphe Settings absent");for(String name:List.of("initialize","neszi_customHomeOriginalInitialize","neszi_keywordOriginalInitialize"))ok(java.util.stream.StreamSupport.stream(settings.getMethods().spliterator(),false).anyMatch(m->m.getName().equals(name)),"settings chain absent "+name);
 ok(bridges>50,"native NSFW traversal bridges missing");ok(hooks.getOrDefault("filterCommentState",0)==2,"both comment projections required");ok(hooks.getOrDefault("filterSearchItems",0)==2,"both search child lists required");
 System.out.println("PASS "+checks+" static assertions; "+cs.size()+" unique classes, "+customClasses+" custom extension classes, "+bridges+" NSFW bridges, "+refs+" injected references resolved.");System.out.println("HOOKS "+hooks);System.out.println("DEX structural checks only; no ART or Android UI execution.");
 }
}
