package app.morphe.extension.newx.anonymous;
import java.nio.file.*;
import app.morphe.extension.shared.Utils;
/** Guards preference namespace collisions, loss of the salt, and ineffective live toggle changes. */
public class HostRuntimeTests {
 static int checks;static void ok(boolean b,String msg){checks++;if(!b)throw new AssertionError(msg);}
 public static void main(String[] a)throws Exception{
 Utils.directory=Path.of(a[0]);Files.createDirectories(Utils.directory);
 Path old=Utils.directory.resolve("piko_advanced_filter.properties");if(!Files.exists(old))Files.writeString(old,"keywords=ExempleInterdit\n");String reference=Files.readString(old);
 ok(Names.initialize(),"Initialize preferences");String id="1234567890123456789";String alias=Names.render("ExempleInterdit",id,0),handle=Names.render("@actual",id,2);
 Path snapshot=Utils.directory.resolve("aliases.txt");String value=alias+"\n"+handle;
 if(Files.exists(snapshot))ok(value.equals(Files.readString(snapshot)),"Stable across separate JVM processes and stored salt");else Files.writeString(snapshot,value);
 ok(!alias.equals("ExempleInterdit")&&!alias.contains(id),"Name anonymous");ok(handle.startsWith("@anonyme_")&&!handle.contains(id),"Handle anonymous");
 Names.set("names",false);ok(Names.render("ExempleInterdit",id,0).equals("ExempleInterdit"),"Name independent disable");ok(Names.render("@actual",id,2).equals(handle),"Handle stays enabled");
 Names.set("names",true);Names.set("handles",false);ok(Names.render("@actual",id,2).equals("@actual"),"Handle independent disable");ok(Names.render("ExempleInterdit",id,0).equals(alias),"Name stays enabled");
 Names.set("handles",true);Names.set("enabled",false);ok(Names.render("ExempleInterdit",id,0).equals("ExempleInterdit")&&Names.render("@actual",id,2).equals("@actual"),"Master disable");
 Names.set("enabled",true);ok(Names.render("ExempleInterdit",id,0).equals(alias),"Re-enable same alias");
 ok(Names.render("unknown",null,0).equals("unknown"),"Missing identity safe");ok(Names.render(null,"0",0)==null,"Null name invalid ID safe");ok(Names.render("@x",id,1).equals(handle.substring(1)),"Raw handle omits @");
 Object user=new Object();Object wrapped=Names.wrap(user);ok(((PresentationUser)wrapped).original==user&&Names.wrap(wrapped)==wrapped,"Presentation retains same original; no nesting");
 ok(Files.readString(old).equals(reference),"Existing filter preferences untouched");
 System.out.println("PASS "+checks+" runtime assertions with host-only identity/preferences adapters; phase="+a[1]);
 }
}
