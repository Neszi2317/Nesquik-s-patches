package app.neszi.asknative;
import java.util.*;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.youtube.patches.ask.AskFlyoutMenuPatch;
public class MenuCaptureTest {
 static byte[] f(int n,byte[] b){return AskNative.field(n,b);}static byte[] s(String v){return AskNative.utf8(v);}
 static byte[] c(byte[]...v){return NativeProtoTest.cat(v);}static void ok(boolean v,String m){NativeProtoTest.check(v,m);}
 static byte[] variant(byte[] item,int outer){return f(outer,AskNative.first(item,66441155));}
 static List<byte[]> items(byte[] menu){List<byte[]> r=new ArrayList<>();for(var f:AskNative.fields(menu))if(f.n==1)r.add(f.v);return r;}
 static void resetMemory() throws Exception {
  AskNative.nativeMenuVisual=null;AskNative.menuBefore="";AskNative.menuAfter="";
  var loaded=AskNative.class.getDeclaredField("menuLoaded");loaded.setAccessible(true);loaded.set(null,false);
 }
 public static class Row implements AskFlyoutMenuPatch.MenuRowAccess {
  final AskFlyoutMenuPatch.MenuItemAccess item;Row(Object item){this.item=(AskFlyoutMenuPatch.MenuItemAccess)item;}
  public AskFlyoutMenuPatch.MenuItemAccess patch_getMenuItem(){return item;}
 }
 public static class Popup implements AskFlyoutMenuPatch.MenuAccess {
  public final List<Object> rows=new ArrayList<>();public boolean dismissed;
  public List<Object> patch_getRows(){return rows;}
  public Object patch_createRow(AskFlyoutMenuPatch.MenuItemAccess item){return new Row(item);}
  public boolean patch_dismissPopup(){dismissed=true;return true;}
 }
 public static void main(String[]args)throws Exception {
  Utils.CONTEXT.resources.config.smallestScreenWidthDp=700;Utils.CONTEXT.saved.clear();resetMemory();
  // Deliberately different outer renderer: native menus are not all service items.
  byte[] share=variant(TabletMenuTest.item(48,"Partager"),90000001);
  byte[] dislike=variant(TabletMenuTest.item(7,"Je n’aime pas"),90000002);
  byte[] save=variant(TabletMenuTest.item(59,"Enregistrer"),90000003);
  byte[] nativeAsk=variant(TabletMenuTest.item(1090,"Posez une question"),90000004);
  byte[] r=AskNative.menuRenderer(nativeAsk);
  byte[] endpoint=c(f(1,s("PAyouchat")),f(3,s("OLD_VIDEO_AND_TOKEN")));
  r=c(AskNative.without(r,3),f(3,f(138681778,endpoint)),f(77,s("DO_NOT_CACHE")));
  nativeAsk=f(90000004,r);
  // Learn a placement that differs from the fallback, demonstrating actual position learning.
  byte[] eligible=c(f(1,dislike),f(1,share),f(1,save),f(1,nativeAsk));
  var msg=new RouteTest.Message(eligible);Popup popup=new Popup();
  ok(AskNative.onNativeMenuOpening(popup,msg)==msg,"native eligible menu preserved byte for byte");
  ok(AskNative.nativeMenuVisual!=null,"native mixed-renderer Ask captured");
  ok(Utils.CONTEXT.saved.containsKey("tablet_menu_visual_v1"),"persisted in app preferences");
  byte[] visual=AskNative.nativeMenuVisual;
  ok(AskNative.first(visual,3).length==0 && AskNative.fields(visual).size()==2,"only title and icon persisted");
  ok(!new String(visual,java.nio.charset.StandardCharsets.ISO_8859_1).contains("DO_NOT_CACHE"),"unknown metadata dropped");
  resetMemory();
  byte[] unavailable=c(f(1,dislike),f(1,share),f(1,save));
  var restored=(RouteTest.Message)AskNative.onNativeMenuOpening(popup,new RouteTest.Message(unavailable));
  List<byte[]> output=items(restored.b);
  ok(output.size()==4 && AskNative.ownMenuItem(output.get(3)),"restart loads cache and restores after actual learned neighbour Save");
  ok(Arrays.equals(output.get(0),dislike)&&Arrays.equals(output.get(1),share)&&Arrays.equals(output.get(2),save),"other native items unchanged");
  ok(AskNative.menuLabel(AskNative.menuRenderer(output.get(3))).equals("posez une question"),"native label retained");
  String marker=AskNative.text(AskNative.first(AskNative.first(AskNative.first(output.get(3),66441155),3),AskNative.MARKER));
  ok(marker.equals(AskNative.PREFIX+"zyxwvutsrqp"),"fresh current video command, never captured command");
  AskFlyoutMenuPatch.executor=b->{};
  ok(AskNative.onNativeMenuTap(new RouteTest.Message(output.get(3)))&&popup.dismissed,"own native popup dismissed before opening");
  ok(!AskFlyoutMenuPatch.autoSummary(),"no automatic summary");
  ok(AskNative.onNativeMenuOpening(popup,restored)==restored,"restoration idempotent");
  byte[] summary=TabletMenuTest.item(1090,"Résumer la vidéo");
  byte[] menuWithSummary=c(f(1,dislike),f(1,summary),f(1,share),f(1,save));
  ok(items(AskNative.transformMenuBytes(menuWithSummary,"abcdefghijk",false)).size()==5,"official summarize SPARK does not suppress Ask");
  byte[] feed=c(f(1,share),f(1,save),f(1,nativeAsk));
  ok(AskNative.transformMenuBytes(feed,"abcdefghijk",false)==feed,"feed popup not classified as watch");
  byte[] commandOnly=c(unavailable,AskNative.numberField(19,1));
  ok(AskNative.transformMenuBytes(commandOnly,"abcdefghijk",false)==commandOnly,"command-only menu preserved");
  Popup wrapped=new Popup();
  for(byte[] b:new byte[][]{dislike,share,save})wrapped.rows.add(new Row(new RouteTest.Message(b)));
  AskNative.onNativePopupShown(wrapped);
  ok(wrapped.rows.size()==4 && wrapped.rows.get(3) instanceof Row,"MenuRowAccess creates correct native row, not a bare protobuf");
  AskNative.onNativePopupShown(wrapped);ok(wrapped.rows.size()==4,"wrapped popup idempotent");
  Utils.CONTEXT.resources.config.smallestScreenWidthDp=400;
  var phone=new RouteTest.Message(unavailable);ok(AskNative.onNativeMenuOpening(popup,phone)==phone,"phone untouched");
  System.out.println("PASS native menu entry before display, mixed renderers, persistent native visual/relative placement, no stale commands, correct video, wrapped rows, summary/feed/phone isolation (JVM fixtures, not Android UI)");
 }
}
