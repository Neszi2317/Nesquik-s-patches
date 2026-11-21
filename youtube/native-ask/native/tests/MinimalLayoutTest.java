package app.neszi.asknative;
import java.util.*;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.youtube.patches.ask.AskFlyoutMenuPatch;
public class MinimalLayoutTest {
 static byte[] f(int n,byte[]b){return AskNative.field(n,b);}static byte[] s(String v){return AskNative.utf8(v);}
 static byte[] c(byte[]...b){return NativeProtoTest.cat(b);}static byte[] g(byte[] b,int n){return AskNative.first(b,n);}
 static byte[] n(int k,long v){return AskNative.numberField(k,v);}static void ok(boolean b,String m){NativeProtoTest.check(b,m);}
 static byte[] content(String icon,String id){return f(AskNative.SECONDARY,c(f(2,s(icon)),f(3,s("Titre original")),f(17,s(id)),n(11,3),n(13,4),n(40,1),f(4,f(169495254,f(901,s("ORIGINAL_COMMAND"))))));}
 static byte[] share=content("yt_outline_share","id.video.share.button");
 static byte[] save=f(484598023,f(2,s("save_this_video_entity")));
 static byte[] like=f(484585818,f(3,s("like_entity"))),dislike=f(484590786,f(2,s("dislike_entity")));
 static byte[] extra=content("yt_outline_download","download");
 static byte[] ask=c(AskNative.without(g(content("yt_fill_spark","native.ask"),AskNative.SECONDARY),4),f(4,f(169495254,f(138681778,c(f(1,s("PAyouchat")),n(5,1))))));
 static byte[] bar(boolean eligible) {
  byte[] fourth=eligible?f(AskNative.SECONDARY,ask):save;
  byte[] overflow=c(f(1,f(AskNative.SECONDARY,content("yt_more","more"))),f(2,extra),eligible?f(2,save):new byte[0],f(3,like),f(3,dislike),f(3,share),f(3,fourth),n(4,1));
  return c(f(1,f(10,like)),f(1,f(10,dislike)),f(1,f(10,share)),f(1,f(10,fourth)),f(1,f(10,f(37098,overflow))),n(3,6),f(7,n(4,5)),f(80,s("keep")));
 }
 static byte[] element(byte[] bar){return f(1,f(168777401,c(f(3,f(172035250,f(1,s("https://www.youtube.com/s/compactify_video_action_bar.eml")))),f(5,f(413471385,f(1,f(2185,bar)))))));}
 static byte[] data(byte[] e){return g(g(g(g(g(g(e,1),168777401),5),413471385),1),2185);}
 static byte[] root(byte[] e,String id){return f(7,f(51779735,f(1,f(49399797,f(1,f(216561405,c(f(1,f(153515154,f(172660663,e))),f(2,s(id)))))))));}
 static byte[] elementFromRoot(byte[] r){for(int n:new int[]{7,51779735,1,49399797,1,216561405,1,153515154,172660663})r=g(r,n);return r;}
 static void reset() throws Exception {AskNative.minimalVisual=null;var p=AskNative.class.getDeclaredField("minimalLoaded");p.setAccessible(true);p.set(null,false);}
 public static void main(String[]a)throws Exception {
  Utils.CONTEXT.resources.config.smallestScreenWidthDp=400;Utils.CONTEXT.saved.clear();reset();
  byte[] original=bar(true),e=element(original);
  byte[] captured=AskNative.transformNextBytes(root(e,"abcdefghijk"));
  byte[] bound=elementFromRoot(captured);
  ok(Arrays.equals(data(bound),original),"genuine native visible actions unchanged");
  ok(AskNative.text(g(bound,AskNative.ELEMENT_VIDEO)).equals("abcdefghijk"),"URI-only element bound to response video");
  ok(AskNative.minimalVisual!=null && Utils.CONTEXT.saved.containsKey("phone_minimal_visual_v1"),"minimal original visual/position persisted");
  ok(AskNative.number(AskNative.minimalVisual,40)==1,"original minimal icon style retained");
  ok(g(AskNative.minimalVisual,4).length==0,"no old tap command in persistent visual");
  reset();
  byte[] raw=bar(false);byte[] restored=AskNative.minimalBar(raw,"zyxwvutsrqp");
  List<byte[]> actions=AskNative.repeated(restored,1);
  ok(actions.size()==5,"same number of slots incl overflow");
  byte[] visibleAsk=g(actions.get(3),10);ok(AskNative.ownContent(visibleAsk),"Ask occupies native slot after Share");
  ok(g(g(visibleAsk,AskNative.SECONDARY),3).length==0,"phone icon only");
  ok(Arrays.equals(g(actions.get(0),10),like)&&Arrays.equals(g(actions.get(1),10),dislike)&&Arrays.equals(g(actions.get(2),10),share),"like dislike share preserved exactly");
  byte[] overflow=g(g(actions.get(4),10),37098);
  ok(AskNative.repeated(overflow,2).stream().anyMatch(x->Arrays.equals(x,save)),"Save retained with actual entity in overflow");
  ok(AskNative.repeated(overflow,2).stream().anyMatch(x->Arrays.equals(x,extra)),"other overflow command retained");
  ok(AskNative.repeated(overflow,2).stream().noneMatch(AskNative::askContent),"no redundant Ask menu entry");
  ok(AskNative.repeated(overflow,3).size()==4 && AskNative.ownContent(AskNative.repeated(overflow,3).get(3)),"shownItems mirror visible row");
  ok(Arrays.equals(restored,AskNative.minimalBar(restored,"zyxwvutsrqp")),"idempotent native minimal transformation");
  var command=new RouteTest.Message(g(g(g(visibleAsk,AskNative.SECONDARY),4),169495254));
  AskNative.route(command,(AskFlyoutMenuPatch.CommandExecutorAccess)b->{});
  ok("zyxwvutsrqp".equals(AskFlyoutMenuPatch.opened)&&!AskFlyoutMenuPatch.autoSummary(),"click bound to new response video, no auto summary");
  byte[] unbound=element(raw);ok(AskNative.afterActionBarLayout(unbound)==unbound,"unbound element cannot inject current player stale ID");
  byte[] boundRaw=c(unbound,f(AskNative.ELEMENT_VIDEO,s("abcdefghijk")));
  byte[] post=AskNative.afterActionBarLayout(boundRaw);
  ok(AskNative.ownContent(g(AskNative.repeated(data(post),1).get(3),10)),"post-conversion callback restores proper slot");
  var nativeCommand=new RouteTest.Message(f(138681778,c(f(1,s("PAyouchat")),n(6,1))));
  ok(AskNative.route(nativeCommand,(AskFlyoutMenuPatch.CommandExecutorAccess)b->{})==nativeCommand,"native opening command not modified");
  ok(AskNative.number(AskNative.nativeWatchPresentation,6)==1,"actual tap presentation captured");
  byte[] portrait=AskNative.nativeWatchPresentation;
  Utils.CONTEXT.resources.config.orientation=2;
  var landscapeCommand=new RouteTest.Message(f(138681778,c(f(1,s("PAyouchat")),n(6,2))));
  AskNative.route(landscapeCommand,(AskFlyoutMenuPatch.CommandExecutorAccess)b->{});
  ok(Arrays.equals(portrait,AskNative.nativeWatchPresentation),"landscape native tap cannot overwrite portrait presentation");
  Utils.CONTEXT.resources.config.orientation=1;
  byte[] unrelated=f(1,s("feed card"));ok(AskNative.afterActionBarLayout(unrelated)==unrelated,"unrelated Elements unchanged");
  Utils.CONTEXT.resources.config.smallestScreenWidthDp=700;
  ok(AskNative.minimalBar(raw,"abcdefghijk")==raw,"tablet path unaffected");
  System.out.println("PASS URI-only binding, minimal native capture/cache reload, slot conservation, Save in overflow, shownItems synchronization, fresh video click, native tap capture, phone/tablet isolation (schema-backed fixtures; no Android UI)");
 }
}
