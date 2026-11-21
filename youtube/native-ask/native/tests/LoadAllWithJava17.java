import java.io.File;import java.util.*;import app.morphe.patcher.patch.PatchKt;
public class LoadAllWithJava17 {
 public static void main(String[] args){
  if(Runtime.version().feature()!=17)throw new AssertionError("Java17 required");
  Set<File> files=new LinkedHashSet<>();for(String a:args)files.add(new File(a));
  var loader=PatchKt.loadPatchesFromJar(files);Set<String> names=new HashSet<>();for(var p:loader)names.add(p.getName());
  for(String n:List.of("Ask flyout menu","Hide video action buttons","Universal auto-translated captions","ZZ Ask native action button"))if(!names.contains(n))throw new AssertionError("Missing "+n);
  System.out.println("PASS real Java17 combined PatchLoader, "+loader.size()+" patches from "+files.size()+" bundles; all four required names present");
 }
}
