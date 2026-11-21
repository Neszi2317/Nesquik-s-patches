import java.io.File;
import java.util.Set;
import app.morphe.patcher.patch.PatchKt;
public class LoadWithJava17 {
 public static void main(String[] args) {
  if(Runtime.version().feature()!=17)throw new AssertionError("Run with a real Java17 runtime");
  var patches=PatchKt.loadPatchesFromJar(Set.of(new File(args[0])));
  if(patches.size()!=1)throw new AssertionError("Expected single-patch bundle, got "+patches.size());
  for(var p:patches)System.out.println("PASS Java "+Runtime.version()+" native Morphe PatchLoader: "+p.getName());
 }
}
