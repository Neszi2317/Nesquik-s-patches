import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.instruction.*;import java.io.*;import java.util.*;
class VerifyDex {
 public static void main(String[]args)throws Exception {
  var ct=DexFileFactory.loadDexContainer(new File(args[0]),Opcodes.getDefault());
  int observe=0,bind=0,ask=0,root=0;Set<String>helpers=new HashSet<>();
  for(var entry:ct.getDexEntryNames())for(var cls:ct.getEntry(entry).getDexFile().getClasses()) {
   if(cls.getType().startsWith("Lapp/neszi/askdescription/") && !helpers.add(cls.getType()))throw new AssertionError("duplicate helper");
   for(var m:cls.getMethods())if(m.getImplementation()!=null) {
    var ins=new ArrayList<Instruction>();m.getImplementation().getInstructions().forEach(ins::add);
    for(int i=0;i<ins.size();i++) {
     if(!(ins.get(i) instanceof ReferenceInstruction r))continue;String ref=r.getReference().toString();
     if(ref.equals("Lapp/neszi/askdescription/DescriptionNative;->observe(Ljava/lang/Object;)V")) {
      observe++;if(ins.get(i+1).getOpcode()!=Opcode.RETURN_OBJECT)throw new AssertionError("observer not at return");
      if(((RegisterRangeInstruction)ins.get(i)).getStartRegister()!=((OneRegisterInstruction)ins.get(i+1)).getRegisterA())throw new AssertionError("return register");
     }
     if(ref.equals("Lapp/neszi/askdescription/DescriptionNative;->beforeBind(Ljava/lang/Object;)Ljava/lang/Object;")) {
      bind++;if(i!=0 || ins.get(1).getOpcode()!=Opcode.MOVE_RESULT_OBJECT || ins.get(2).getOpcode()!=Opcode.CHECK_CAST)throw new AssertionError("binder hook");
      int p1=m.getImplementation().getRegisterCount()-3;
      if(((RegisterRangeInstruction)ins.get(0)).getStartRegister()!=p1 || ((OneRegisterInstruction)ins.get(1)).getRegisterA()!=p1)throw new AssertionError("panel register");
      System.out.println("PASS native binder "+m);
     }
     if(ref.contains("AskNative;->transformParsed"))ask++;
     if(cls.getType().equals("Lapp/neszi/askdescription/DescriptionNative;") && m.getName().equals("isWatchRoot") && ins.get(i).getOpcode()==Opcode.INSTANCE_OF)root++;
     if(cls.getType().startsWith("Lapp/neszi/askdescription/") && (ref.contains("Landroid/view/") || ref.contains("Landroid/widget/") || ref.contains("Landroid/graphics/") || ref.contains("openAskPanel") || ref.contains("DescriptionTest")))throw new AssertionError("custom UI/click/test reference "+ref);
    }
   }
  }
  if(bind!=1 || observe!=15 || root!=1 || helpers.size()<2 || (args.length>1 && ask!=15))throw new AssertionError("hooks: "+bind+","+observe+","+root+","+ask);
  System.out.println("PASS observation returns="+observe+", binder="+bind+", Ask0.8.2 parser hooks="+ask+", helper types="+helpers.size()+"; no custom UI or click handler");
 }
}
