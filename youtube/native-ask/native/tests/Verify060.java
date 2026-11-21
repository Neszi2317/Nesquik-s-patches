import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.io.*;import java.util.*;
public class Verify060 {
 static String ref(Instruction i){return i instanceof ReferenceInstruction r?r.getReference().toString():"";}
 public static void main(String[]a)throws Exception{
  var ct=DexFileFactory.loadDexContainer(new File(a[0]),Opcodes.getDefault());int returns=0;boolean panel=false,helper=false,reflection=false;
  for(String e:ct.getDexEntryNames()) for(var c:ct.getEntry(e).getDexFile().getClasses()) {
   if(c.getType().equals("Lapp/neszi/asknative/AskNative;"))helper=true;
   if(c.getType().endsWith("/ask/AskPanelCommand;")) {
    var names=new HashSet<String>();for(var f:c.getFields())names.add(f.getName());
    if(!names.containsAll(List.of("templateVideoId","templatePanel","lastSelectionEntityKey")))throw new AssertionError("official private fields missing");
    for(var m:c.getMethods())if(m.getName().equals("retarget")&&m.getParameterTypes().toString().equals("[Ljava/lang/String;, Ljava/lang/String;, Ljava/lang/String;, I, [I]"))reflection=true;
   }
   if(c.getType().contains("OriginalVisualPanelTest")||c.getType().contains("NativeProtoTest"))throw new AssertionError("test code in APK");
   for(var m:c.getMethods())if(m.getImplementation()!=null){
    var ins=new ArrayList<Instruction>();m.getImplementation().getInstructions().forEach(ins::add);
    if(c.getType().endsWith("/AskFlyoutMenuPatch;")&&m.getName().equals("showAskPanel")){
     if(!ref(ins.get(0)).contains("->beginShow(Ljava/lang/String;)V"))throw new AssertionError("begin hook");
     int adapt=-1,call=-1,before=-1;
     for(int i=0;i<ins.size();i++){
      if(ref(ins.get(i)).contains("->adaptPanelCommand([B)[B"))adapt=i;
      if(ref(ins.get(i)).contains("->patch_executeCommand([B)V"))call=i;
      if(ref(ins.get(i)).contains("->beforeShow()V"))before=i;
     }
     if(!(before>=0&&adapt>before&&call==adapt+2))throw new AssertionError("panel hook order");
     int r=((RegisterRangeInstruction)ins.get(adapt)).getStartRegister();
     if(((OneRegisterInstruction)ins.get(adapt+1)).getRegisterA()!=r||((FiveRegisterInstruction)ins.get(call)).getRegisterD()!=r)throw new AssertionError("byte-array register");
     panel=true;System.out.println("PASS native panel command hook order and byte[] register v"+r);
    }
    if((m.getName().equals("parseFrom")||m.getName().equals("parsePartialFrom"))&&ins.stream().anyMatch(i->ref(i).contains("->transformParsed")))for(int i=0;i<ins.size();i++)if(ins.get(i).getOpcode()==Opcode.RETURN_OBJECT){
     if(i<3||!ref(ins.get(i-3)).contains("->transformParsed")||ins.get(i-2).getOpcode()!=Opcode.MOVE_RESULT_OBJECT||ins.get(i-1).getOpcode()!=Opcode.CHECK_CAST)throw new AssertionError("unhooked return");returns++;
    }
   }
  }
  if(!panel||!helper||!reflection||returns<14)throw new AssertionError("missing hooks");
  System.out.println("PASS "+returns+" root decode returns, official reflection targets present, no tests shipped");
 }
}
