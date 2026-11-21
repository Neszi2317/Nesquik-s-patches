import java.io.*;import java.util.*;import java.util.regex.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import com.android.tools.smali.dexlib2.iface.reference.*;
public class DexInspect {
 public static void main(String[] args)throws Exception{
  var dex=DexFileFactory.loadDexContainer(new File(args[0]),Opcodes.getDefault());String mode=args[1];Pattern p=Pattern.compile(args[2],Pattern.CASE_INSENSITIVE);
  for(String n:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(n).getDexFile().getClasses()){
   if(mode.equals("types")){if(p.matcher(c.getType()+" "+c.getSuperclass()+" "+c.getInterfaces()).find())System.out.println(c.getType()+" extends "+c.getSuperclass()+" "+c.getInterfaces());continue;}
   if(mode.equals("class")&&!p.matcher(c.getType()).find())continue;
   if(mode.equals("class")){System.out.println("CLASS "+c.getType()+" extends "+c.getSuperclass()+" interfaces="+c.getInterfaces());for(Field f:c.getFields())System.out.println("FIELD "+f);}
   for(Method m:c.getMethods()){
    if(mode.equals("class")){System.out.println("METHOD "+m+" flags="+m.getAccessFlags());if(m.getImplementation()!=null)System.out.println("REGISTERS "+m.getImplementation().getRegisterCount());}
    if(m.getImplementation()==null)continue;int off=0;
    for(Instruction i:m.getImplementation().getInstructions()){
     String ref=i instanceof ReferenceInstruction r?r.getReference().toString():"";
     if(mode.equals("class")){String r="";
      if(i instanceof OneRegisterInstruction x)r+=" A="+x.getRegisterA();if(i instanceof TwoRegisterInstruction x)r+=" B="+x.getRegisterB();if(i instanceof ThreeRegisterInstruction x)r+=" C="+x.getRegisterC();
      if(i instanceof FiveRegisterInstruction x)r+=" regs="+x.getRegisterC()+","+x.getRegisterD()+","+x.getRegisterE()+","+x.getRegisterF()+","+x.getRegisterG()+" count="+x.getRegisterCount();
      if(i instanceof RegisterRangeInstruction x)r+=" start="+x.getStartRegister()+" count="+x.getRegisterCount();if(i instanceof NarrowLiteralInstruction x)r+=" lit="+x.getNarrowLiteral();if(i instanceof OffsetInstruction x)r+=" target="+(off+x.getCodeOffset());
      System.out.println(off+" "+i.getOpcode()+" "+ref+r);
     }else if(!ref.isEmpty()&&p.matcher(ref).find()&&(!mode.equals("strings")||((ReferenceInstruction)i).getReference() instanceof StringReference))System.out.println(m+" @"+off+" "+i.getOpcode()+" "+ref);
     off+=i.getCodeUnits();
    }
   }
  }
 }
}
