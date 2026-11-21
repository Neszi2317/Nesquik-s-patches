package app.neszi.askdescription;
import java.util.*;import java.io.*;import static app.neszi.askdescription.Wire.*;
import app.morphe.extension.youtube.patches.ask.AskFlyoutMenuPatch.MessageAccess;
import app.morphe.extension.shared.Utils;import app.morphe.extension.youtube.patches.VideoInformation;
public class DescriptionTest {
 static int checks;
 static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 static byte[] join(byte[]...a){ByteArrayOutputStream o=new ByteArrayOutputStream();for(byte[]v:a)write(o,v);return o.toByteArray();}
 static byte[] scalar(int n,int v){ByteArrayOutputStream o=new ByteArrayOutputStream();varint(o,n*8);varint(o,v);return o.toByteArray();}
 static final String VIDEO="abcDEF123_-", OTHER="ZYXwvu987_-";
 static final byte[] COMMAND=field(169495254,field(138681778,join(field(1,utf8("PAyouchat")),field(3,utf8(VIDEO)))));
 // Synthetic fixture. Actual renderer field chain comes from the supplied APK.
 static byte[] footer(){return field(260791572,field(1,field(153515154,join(field(1,utf8("input_composer_button.eml")),field(2,utf8("Résume la vidéo")),field(3,COMMAND)))));}
 static byte[] panel(boolean footer){return join(field(1,utf8(DescriptionNative.PANEL)),field(2,utf8("native-header")),field(3,utf8("current-description")),field(123456,utf8("unknown-preserved")),footer?field(10,footer()):new byte[0],scalar(47,2),scalar(39,1));}
 static byte[] root(String id,byte[]p){return join(field(7,field(51779735,field(1,field(49399797,field(1,field(216561405,field(2,utf8(id)))))))),field(25,field(138681066,p)));}
 public static final class NativePanel implements MessageAccess {final byte[] b;public NativePanel(byte[]b){this.b=b;}public byte[]patch_toByteArray(){return b;}public MessageAccess patch_parseSameType(byte[] b){return new NativePanel(b);}}
 static void clearMemory()throws Exception {var f=DescriptionNative.class.getDeclaredField("memory");f.setAccessible(true);((Map<?,?>)f.get(null)).clear();}
 public static void main(String[]args)throws Exception {
  byte[] genuine=panel(true), empty=panel(false), saved=DescriptionNative.snapshot(genuine);
  check(saved!=null,"recognize exact footer chain");
  byte[] restored=DescriptionNative.restore(empty,saved);
  check(Arrays.equals(restored,genuine),"byte-exact restoration with unchanged panel");
  check(Arrays.equals(first(restored,10),first(genuine,10)),"opaque click bytes preserved");
  for(int n:new int[]{1,2,3,123456})check(Arrays.equals(first(restored,n),first(empty,n)),"unrelated field preserved "+n);
  check(DescriptionNative.restore(genuine,saved)==genuine,"eligible no-op");
  check(DescriptionNative.restore(restored,saved)==restored,"idempotence");
  byte[] other=field(1,utf8("PAyouchat"));check(DescriptionNative.restore(other,saved)==other,"other panel no-op");
  check(DescriptionNative.snapshot(field(10,footer()))==null,"not description");
  byte[] otherFooter=join(empty,field(10,field(444,utf8("newer-native-footer"))));
  check(DescriptionNative.restore(otherFooter,saved)==otherFooter,"preserve other footer");
  check(DescriptionNative.restore(empty,field(10,field(1,utf8("input_composer_button.e"))))==empty,"reject wrong schema");
  check(DescriptionNative.panels(root(VIDEO,genuine)).size()==1,"native panel list");
  check(VIDEO.equals(DescriptionNative.watchVideo(root(VIDEO,genuine))),"response video binding");
  check(DescriptionNative.panels(field(99,field(138681066,genuine))).isEmpty(),"ignore unrelated recommendation");
  DescriptionNative.captureRoot(root(VIDEO,genuine),Utils.CONTEXT);
  check(!Utils.CONTEXT.saved.isEmpty(),"preferences saved");clearMemory();
  VideoInformation.video=VIDEO;NativePanel input=new NativePanel(empty);
  NativePanel result=(NativePanel)DescriptionNative.beforeBind(input);
  check(result!=input && Arrays.equals(first(result.b,10),footer()),"reload persistence after memory reset");
  VideoInformation.video=OTHER;check(DescriptionNative.beforeBind(input)==input,"no other video replay");
  VideoInformation.video=VIDEO;Utils.CONTEXT.res.config.orientation=2;check(DescriptionNative.beforeBind(input)==input,"no orientation leak");Utils.CONTEXT.res.config.orientation=1;
  Utils.CONTEXT.res.config.uiMode=16;check(DescriptionNative.beforeBind(input)==input,"no theme leak");Utils.CONTEXT.res.config.uiMode=32;
  NativePanel malformed=new NativePanel(new byte[]{0});check(DescriptionNative.beforeBind(malformed)==malformed,"malformed preserved");
  clearMemory();Utils.CONTEXT.saved.replaceAll((k,v)->k.equals("keys")?v:"invalid base64 !");check(DescriptionNative.beforeBind(input)==input,"corrupt storage preserved");
  check(DescriptionNative.beforeBind(null)==null,"null preserved");
  System.out.println("PASS "+checks+" fixture assertions; Android display/click NOT tested");
 }
}
