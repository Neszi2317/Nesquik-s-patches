import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.security.*;
import com.android.apksig.ApkVerifier;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
public class Inspect {
 public static Map<String,ClassDef> classes(String path)throws Exception {
  var result=new HashMap<String,ClassDef>();var c=DexFileFactory.loadDexContainer(new File(path),Opcodes.getDefault());
  for(var n:c.getDexEntryNames())for(var x:c.getEntry(n).getDexFile().getClasses())result.put(x.getType(),x);return result;
 }
 static String dump(ClassDef c){
  var out=new StringBuilder(c.getType()+"\n");var methods=new TreeMap<String,Method>();for(var m:c.getMethods())methods.put(m.toString(),m);
  for(var m:methods.values()){
   out.append(m.toString()).append(" flags=").append(m.getAccessFlags()).append('\n');if(m.getImplementation()==null)continue;
   out.append("registers=").append(m.getImplementation().getRegisterCount()).append('\n');int pc=0;
   for(var i:m.getImplementation().getInstructions()){
    out.append(pc).append(' ').append(i.getOpcode());
    if(i instanceof OneRegisterInstruction a)out.append(" A=").append(a.getRegisterA());
    if(i instanceof TwoRegisterInstruction b)out.append(" B=").append(b.getRegisterB());
    if(i instanceof ThreeRegisterInstruction d)out.append(" C=").append(d.getRegisterC());
    if(i instanceof FiveRegisterInstruction a)out.append(" regs=").append(Arrays.toString(new int[]{a.getRegisterC(),a.getRegisterD(),a.getRegisterE(),a.getRegisterF(),a.getRegisterG()})).append(" count=").append(a.getRegisterCount());
    if(i instanceof OffsetInstruction a)out.append(" offset=").append(a.getCodeOffset());
    if(i instanceof NarrowLiteralInstruction a)out.append(" literal=").append(a.getNarrowLiteral());
    if(i instanceof ReferenceInstruction a)out.append(" ref=").append(a.getReference());
    out.append('\n');pc+=i.getCodeUnits();
   }
  }return out.toString();
 }
 public static void main(String[] a)throws Exception{
  if(a[0].equals("sig")){
   for(int i=1;i<a.length;i++){var r=new ApkVerifier.Builder(new File(a[i])).build().verify();System.out.println(a[i]+" verified="+r.isVerified()+" v2="+r.isVerifiedUsingV2Scheme()+" v3="+r.isVerifiedUsingV3Scheme()+" errors="+r.getErrors());if(!r.isVerified())throw new AssertionError("Signature invalid");for(var c:r.getSignerCertificates())System.out.println("cert SHA256="+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(c.getEncoded())));}
  }else{
   var c=classes(a[0]);for(var x:c.values())for(var m:x.getMethods())if(m.getName().equals("morphe_completeTranslations")){Files.writeString(Path.of(a[1]),dump(x));System.out.println("Caption manager="+x.getType());return;}throw new AssertionError("Missing helper");
  }
 }
}
