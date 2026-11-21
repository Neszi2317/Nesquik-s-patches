package app.neszi.asknative;
import java.util.*;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.youtube.patches.ask.AskFlyoutMenuPatch;
public class TabletMenuTest {
 static byte[] f(int n,byte[]b){return AskNative.field(n,b);}static byte[] s(String x){return AskNative.utf8(x);}
 static byte[] c(byte[]...bs){return NativeProtoTest.cat(bs);}static byte[] get(byte[]b,int n){return AskNative.first(b,n);}
 static void ok(boolean b,String m){NativeProtoTest.check(b,m);}
 static byte[] item(int icon,String text){return f(66441155,c(f(1,f(1,f(1,s(text)))),f(2,AskNative.numberField(1,icon)),f(3,f(999,s("KEEP_"+text)))));}
 public static class Popup implements AskFlyoutMenuPatch.MenuAccess {
  public final List<Object> rows=new ArrayList<>();
  public List<Object> patch_getRows(){return rows;} public boolean patch_dismissPopup(){return true;}
 }
 public static void main(String[]args){
  byte[] share=item(48,"Partager"),save=item(59,"Enregistrer"),dislike=item(7,"Je n'aime pas");
  byte[] menu=c(f(1,dislike),f(1,share),f(1,save),f(80,s("unchanged")));
  byte[] compact=f(42,menu);
  Utils.CONTEXT.resources.config.smallestScreenWidthDp=700;
  byte[] output=AskNative.insert(compact,"abcdefghijk",true);
  var rows=AskNative.fields(get(output,42));
  ok(rows.size()==5,"one menu entry added");
  ok(Arrays.equals(rows.get(0).v,dislike)&&Arrays.equals(rows.get(1).v,share)&&Arrays.equals(rows.get(3).v,save),"menu order and existing actions preserved");
  byte[] ask=rows.get(2).v;ok(AskNative.number(get(get(ask,66441155),2),1)==1090,"native sparkle enum");
  ok(AskNative.text(get(get(get(get(ask,66441155),1),1),1)).equals("Posez une question"),"native menu label");
  ok(AskNative.fields(output).size()==1,"tablet menu only; no forced top-row button");
  ok(Arrays.equals(output,AskNative.insert(output,"abcdefghijk",true)),"deduplicated own menu");
  boolean[] found={false};byte[] nativeMenu=c(f(1,share),f(1,item(1090,"Ask")),f(1,save));
  ok(AskNative.tabletMenus(nativeMenu,"abcdefghijk",0,found)==nativeMenu&&found[0],"genuine Ask preserved");
  AskFlyoutMenuPatch.executor=b->{};
  ok(AskNative.onNativeMenuTap(new RouteTest.Message(ask)),"native menu tap consumed");
  ok(AskFlyoutMenuPatch.opened.equals("abcdefghijk")&&!AskFlyoutMenuPatch.autoSummary(),"current video opened with no summary");
  ok(!AskNative.onNativeMenuTap(new RouteTest.Message(share)),"Share never intercepted");
  AskFlyoutMenuPatch.showAskPanel("abcdefghijk");ok(AskFlyoutMenuPatch.autoSummary(),"official summary preserved after direct tap");
  Popup popup=new Popup();
  for(byte[] b:new byte[][]{dislike,share,save})popup.rows.add(new RouteTest.Message(b));
  AskNative.onNativePopupShown(popup);
  ok(popup.rows.size()==4,"deferred tablet popup gains Ask");
  byte[] dynamic=((RouteTest.Message)popup.rows.get(2)).b;
  ok(AskNative.text(get(get(get(dynamic,66441155),3),AskNative.MARKER)).equals(AskNative.PREFIX+"zyxwvutsrqp"),"deferred menu uses actual current player video");
  AskNative.onNativePopupShown(popup);ok(popup.rows.size()==4,"deferred menu duplicate prevented");
  Popup feedMenu=new Popup();feedMenu.rows.add(new RouteTest.Message(share));feedMenu.rows.add(new RouteTest.Message(save));
  AskNative.onNativePopupShown(feedMenu);ok(feedMenu.rows.size()==2,"ordinary feed menu unchanged");
  Utils.CONTEXT.resources.config.smallestScreenWidthDp=400;
  ok(AskNative.insert(compact,"abcdefghijk",true)==compact,"phone never modifies tablet-only menu");
  byte[] phoneShare=NativeProtoTest.button("yt_outline_share","Partager");
  byte[] single=NativeProtoTest.root(f(1,phoneShare),true,"abcdefghijk","compactify_video_action_bar.e");
  byte[] alt=f(7,f(98765432,single));
  byte[] changed=AskNative.transformNextBytes(alt);
  ok(!Arrays.equals(changed,alt),"watch metadata discovered in alternate primary layout");
  ok(Arrays.equals(changed,AskNative.transformNextBytes(changed)),"alternate layout idempotent");
  byte[] feed=f(55,single);ok(feed==AskNative.transformNextBytes(feed),"other root regions/feed left alone");
  System.out.println("PASS tablet menu exact order/native icon/current-video click/no summary/no forced row, phone isolation, alternate watch contents, idempotence (synthetic JVM; not device UI)");
 }
}
