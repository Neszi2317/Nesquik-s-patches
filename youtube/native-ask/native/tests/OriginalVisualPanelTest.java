package app.neszi.asknative;
import java.util.*;
import java.io.*;
import app.morphe.extension.shared.Utils;
public class OriginalVisualPanelTest {
 static byte[] f(int n,byte[] b){return AskNative.field(n,b);}
 static byte[] s(String s){return AskNative.utf8(s);}
 static byte[] cat(byte[]...bs){return NativeProtoTest.cat(bs);}
 static byte[] number(int n,int v){ByteArrayOutputStream out=new ByteArrayOutputStream();AskNative.varint(out,(long)n<<3);AskNative.varint(out,v);return out.toByteArray();}
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  byte[] icon=f(35,f(1,s("original-icon-asset")));
  byte[] authentic=cat(icon,number(7,2),number(11,1),number(13,2),f(3,s("Ask")),f(5,s("Ask a question")),f(4,f(169495254,f(999,s("PRIVATE_ORIGINAL_COMMAND")))),f(999,s("ORIGINAL_TRACKING")));
  AskNative.captureVisual(authentic);
  ok(Arrays.equals(AskNative.first(AskNative.genuineVisual,35),AskNative.first(authentic,35)),"original icon payload preserved exactly");
  String cached=AskNative.text(AskNative.genuineVisual);
  ok(!cached.contains("PRIVATE_ORIGINAL_COMMAND")&&!cached.contains("ORIGINAL_TRACKING"),"only visuals stored, no original command/tracking");
  byte[] phone=AskNative.visualForDevice(AskNative.genuineVisual,false),tablet=AskNative.visualForDevice(AskNative.genuineVisual,true);
  ok(AskNative.first(phone,3).length==0,"phone icon-only with accessibility label");
  ok(AskNative.text(AskNative.first(tablet,3)).equals("Poser une question"),"tablet label exact");
  ok(Arrays.equals(AskNative.first(phone,35),AskNative.first(tablet,35)),"same genuine icon on both layouts");
  byte[] save=f(10,f(484598023,f(2,s("save"))));
  byte[] numericShare=f(10,f(AskNative.SECONDARY,cat(number(48,48),f(5,s("Partager cette vidéo")))));
  byte[] row=cat(f(1,save),f(1,numericShare));
  List<AskNative.Field> out=AskNative.fields(AskNative.insert(row,"abcdefghijk"));
  ok(out.size()==3&&Arrays.equals(out.get(1).v,numericShare),"numeric Share found by accessibility and preserved");
  ok(AskNative.text(AskNative.first(AskNative.first(AskNative.first(out.get(2).v,10),AskNative.SECONDARY),17)).equals("id.neszi.ask.direct"),"Ask positioned after numeric Share");
  // Recover original icon across simulated process restart.
  AskNative.genuineVisual=null;java.lang.reflect.Field loaded=AskNative.class.getDeclaredField("visualLoaded");loaded.setAccessible(true);loaded.setBoolean(null,false);
  AskNative.insert(row,"abcdefghijk");ok(AskNative.genuineVisual!=null,"original icon survives preference reload");
  for(int surface:new int[]{0,1,3}) {
   byte[] nativeId=cat(number(1,surface),f(3,s("PAyouchat")));
   byte[] nativePanel=cat(f(1,s("PAyouchat")),f(18,nativeId),f(99,s("CURRENT_VIDEO_CONTEXTUAL_SUGGESTIONS")));
   byte[] browse=cat(number(1,2),f(3,s("PAyouchat")));
   byte[] command=f(138681778,cat(f(2,f(138681066,cat(f(1,s("PAyouchat")),f(18,nativeId),f(99,s("summarize only"))))),f(10,browse),number(77,5)));
   byte[] rewritten=AskNative.watchCommand(command,nativePanel,null);
   byte[] endpoint=AskNative.first(rewritten,138681778);
   ok(Arrays.equals(AskNative.first(endpoint,10),nativeId),"watch surface copied exactly, never guessed");
   byte[] panel=AskNative.first(AskNative.first(endpoint,2),138681066);
   ok(AskNative.text(AskNative.first(panel,99)).equals("CURRENT_VIDEO_CONTEXTUAL_SUGGESTIONS"),"fresh same-video native suggestions retained");
   ok(AskNative.fields(panel).stream().anyMatch(x->x.n==20&&x.w==0),"native panel replacement flag kept");
   ok(AskNative.fields(endpoint).stream().anyMatch(x->x.n==77&&x.w==0),"unrelated command fields preserved");
   ok(AskNative.adaptPanelCommand(command)==command,"ordinary menu command untouched without direct provenance");
  }
  System.out.println("PASS: genuine visual byte preservation + persistence, no private command reuse, phone icon-only, tablet label, numeric Share position, original watch identifier, fresh current-video suggestions, ordinary menu untouched (JVM fixtures)");
 }
}
