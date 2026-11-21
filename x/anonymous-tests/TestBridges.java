import java.nio.file.*;
import org.jetbrains.org.objectweb.asm.*;
/** Replaces ONLY the release-specific DEX bridge for host tests. Rendering logic stays compiled as shipped. */
public class TestBridges {
 public static void main(String[] a)throws Exception{
  ClassReader r=new ClassReader(Files.readAllBytes(Path.of(a[0])));ClassWriter w=new ClassWriter(0);
  r.accept(new ClassVisitor(Opcodes.ASM9,w){
   public MethodVisitor visitMethod(int ac,String name,String desc,String sig,String[] ex){
    MethodVisitor mv=super.visitMethod(ac,name,desc,sig,ex);
    if(name.equals("userId")){mv.visitCode();mv.visitVarInsn(Opcodes.ALOAD,0);mv.visitMethodInsn(Opcodes.INVOKESTATIC,"java/lang/String","valueOf","(Ljava/lang/Object;)Ljava/lang/String;",false);mv.visitInsn(Opcodes.ARETURN);mv.visitMaxs(1,1);mv.visitEnd();return null;}return mv;
   }
  },0);
  Path out=Path.of(a[1]);Files.createDirectories(out.getParent());Files.write(out,w.toByteArray());
 }
}
