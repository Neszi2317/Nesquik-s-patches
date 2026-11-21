package app.neszi.asknative;
import java.util.*;import app.morphe.extension.shared.Utils;
public class PanelPresentationTest {
 static byte[] f(int n,byte[] b){return AskNative.field(n,b);}static byte[] s(String x){return AskNative.utf8(x);}
 static byte[] n(int n,int x){return OriginalVisualPanelTest.number(n,x);}static byte[] cat(byte[]...b){return NativeProtoTest.cat(b);}
 static byte[] get(byte[]b,int k){return AskNative.first(b,k);}static void ok(boolean x,String m){if(!x)throw new AssertionError(m);}
 public static void main(String[]a)throws Exception{
  byte[] browse=cat(n(1,2),f(3,s("PAyouchat")));
  byte[] currentPanel=cat(f(1,s("PAyouchat")),f(18,browse),f(90,s("CURRENT_VIDEO_CHAT")));
  byte[] generated=f(138681778,cat(f(2,f(138681066,currentPanel)),f(10,browse),n(5,1),n(16,1),f(8,f(1,n(1,0))),n(77,8)));
  byte[] presentation=cat(f(1,s("PAyouchat")),n(6,1),f(8,f(1,cat(n(1,1),f(2,f(999,s("OLD_CALLBACK")))))));
  byte[] source=cat(presentation,f(2,f(138681066,f(90,s("OLD_VIDEO_AND_TOKEN")))),f(3,s("OLD_REQUEST_PARAMS")),f(7,f(999,s("OLD_ON_SHOW_COMMAND"))),f(13,s("OLD_CONFIG")),f(14,s("OLD_BYTES")),f(17,f(999,s("OLD_AFTER_COMMAND"))));
  byte[] serial=f(400000000,cat(f(1,f(98765,s("GESTURE_TRACKING"))),f(1,f(169495254,f(138681778,source)))));
  byte[] captured=AskNative.findWatchPresentation(serial,0);
  ok(captured!=null,"nested serial onTap recognized");
  String raw=AskNative.text(captured);
  ok(!raw.contains("OLD_")&&!raw.contains("GESTURE_TRACKING"),"no old video, request, tokens, analytics or callbacks retained");
  ok(AskNative.findWatchPresentation(f(138681778,f(1,s("another-panel"))),0)==null,"other panels never captured");
  ok(AskNative.findWatchPresentation(new byte[]{10,100,1},0)==null,"malformed opaque values safe");
  byte[] finalCommand=AskNative.watchCommand(generated,null,null,captured);
  byte[] endpoint=get(finalCommand,138681778);
  ok(AskNative.text(get(endpoint,1)).equals("PAyouchat"),"legacy native target1 preserved");
  ok(get(endpoint,10).length==0,"BROWSE identifier10 removed; target oneof not reconstructed");
  ok(AskNative.fields(endpoint).stream().noneMatch(x->x.n==5||x.n==16),"absence of original flags also preserved");
  ok(AskNative.fields(endpoint).stream().anyMatch(x->x.n==6&&x.v[0]==1),"original expansion flag copied");
  ok(get(get(get(endpoint,8),1),2).length==0,"nested layout callback stripped");
  ok(AskNative.text(get(get(get(endpoint,2),138681066),90)).equals("CURRENT_VIDEO_CHAT"),"only current chat panel preserved");
  ok(AskNative.fields(endpoint).stream().anyMatch(x->x.n==77),"unrelated current command retained");
  byte[] identifier=cat(n(1,1),f(3,s("PAyouchat")));
  byte[] sourceModern=cat(f(10,identifier),n(5,0),f(8,f(2,n(1,1))));
  byte[] modern=AskNative.watchCommand(generated,null,null,AskNative.watchPresentation(sourceModern));
  ok(Arrays.equals(get(get(modern,138681778),10),identifier),"modern identifier preserved exactly");
  ok(get(get(modern,138681778),1).length==0,"no competing legacy target");
  // Original appearance capture also persists presentation; original-command data does not survive.
  byte[] genuine=cat(f(35,f(1,s("EXACT_ICON"))),f(4,serial),f(5,s("Ask a question")));
  AskNative.captureVisual(genuine);
  ok(AskNative.nativeWatchPresentation!=null,"same existing capture hook saves layout");
  byte[] saved=AskNative.nativeWatchPresentation;
  AskNative.nativeWatchPresentation=null;AskNative.genuineVisual=null;
  java.lang.reflect.Field loaded=AskNative.class.getDeclaredField("visualLoaded");loaded.setAccessible(true);loaded.setBoolean(null,false);
  AskNative.insert(f(1,f(10,f(484598023,f(2,s("save"))))),"abcdefghijk");
  ok(Arrays.equals(saved,AskNative.nativeWatchPresentation),"presentation survives simulated restart");
  ok(AskNative.adaptPanelCommand(generated)==generated,"official menu never adapted without direct provenance");
  System.out.println("PASS legacy/modern native target, serial command capture, presentation flags and absence, popup callback removal, current-chat preservation, local persistence, other panels/menu untouched (JVM fixtures)");
 }
}
