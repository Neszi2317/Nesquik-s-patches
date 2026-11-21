package app.neszi.asknative;
import java.util.*;
import app.morphe.extension.youtube.patches.ask.AskFlyoutMenuPatch;
public class FullscreenQuickActionsTest {
 static byte[] f(int n,byte[] b){return AskNative.field(n,b);}static byte[] s(String x){return AskNative.utf8(x);}
 static byte[] cat(byte[]...b){return NativeProtoTest.cat(b);}static byte[] get(byte[]b,int n){return AskNative.first(b,n);}
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 static byte[] quick(byte[] model,String name){return f(153515154,f(172660663,cat(f(1,f(168777401,f(5,model))),f(2,f(183314536,f(1,s(name)))))));}
 static byte[] model(byte[] quick){return get(get(get(get(get(quick,153515154),172660663),1),168777401),5);}
 public static void main(String[]args)throws Exception{
  byte[] share=NativeProtoTest.button("yt_outline_share","Partager");
  byte[] comments=NativeProtoTest.button("yt_outline_message_bubble","Commentaires");
  byte[] like=f(10,f(484585818,f(3,s("LIKE_ENTITY"))));
  byte[] overflow=f(10,f(37098,new byte[0]));
  byte[] list=cat(f(6,like),f(6,comments),f(6,share),f(6,overflow),f(99,s("PRESERVE")));
  byte[] inputModel=f(413471385,f(1,f(99999999,list)));
  byte[] eligiblePresentation=f(1,s("PAyouchat"));
  byte[] viewport=cat(f(8,quick(inputModel,"quick_actions.e:variant")),f(7,f(51779735,NativeProtoTest.root(f(1,share),false,"abcdefghijk","unrelated.e"))));
  AskNative.genuineVisual=f(2,s("yt_fill_spark"));
  byte[] alternative=cat(f(37,quick(inputModel,"quick_actions.e")),f(7,get(viewport,7)));
  byte[] fixed=AskNative.transformNextBytes(alternative);
  ok(!Arrays.equals(get(alternative,37),get(fixed,37)),"quick actions outside assumed field8 now found");
  byte[] unrelated=f(42,s("untouched"));
  ok(Arrays.equals(get(AskNative.transformNextBytes(cat(alternative,unrelated)),42),s("untouched")),"other root fields preserved");
  byte[] changed=AskNative.transformNextBytes(viewport);
  byte[] finalList=get(get(get(model(get(changed,8)),413471385),1),99999999);
  var fs=AskNative.fields(finalList);
  ok(fs.size()==6,"exactly one action added");
  ok(Arrays.equals(fs.get(0).v,like)&&Arrays.equals(fs.get(1).v,comments)&&Arrays.equals(fs.get(2).v,share)&&Arrays.equals(fs.get(4).v,overflow),"all old buttons byte-for-byte and Ask after Share before More");
  byte[] ask=get(get(fs.get(3).v,10),AskNative.SECONDARY);
  ok(get(ask,3).length==0,"fullscreen icon-only");
  ok(AskNative.text(get(ask,2)).equals("yt_fill_spark"),"native cached icon used");
  byte[] marker=get(get(ask,4),169495254);
  ok(AskNative.text(get(marker,AskNative.MARKER)).equals(AskNative.FULLSCREEN_PREFIX+"abcdefghijk"),"fullscreen provenance/current video");
  ok(!AskNative.text(ask).contains("ORIGINAL_SHARE_COMMAND"),"Share action never copied");
  ok(Arrays.equals(changed,AskNative.transformNextBytes(changed)),"no duplicate on second decode");
  byte[] other=quick(inputModel,"unrelated.e");
  ok(other==AskNative.walkQuickElements(other,"abcdefghijk",0,new int[]{0}),"other Elements unchanged");
  byte[] only=f(6,share);
  ok(only==AskNative.insertQuickModel(only,"abcdefghijk",0),"single-child unconfirmed container untouched");
  byte[] unsupported=quick(f(12345,f(1,s("UNKNOWN_SCHEMA"))),"quick_actions.e");
  ok(unsupported==AskNative.walkQuickElements(unsupported,"abcdefghijk",0,new int[]{0}),"unknown fullscreen schema untouched");
  byte[] bad=new byte[]{10,100,1};ok(bad==AskNative.walkQuickElements(bad,"abcdefghijk",0,new int[]{0}),"opaque malformed content unchanged");
  byte[] noid=f(8,quick(inputModel,"quick_actions.e"));ok(noid==AskNative.transformNextBytes(noid),"no current video means no insertion");
  byte[] nativeButton=cat(f(2,s("yt_fill_spark")),f(4,f(169495254,f(138681778,eligiblePresentation))));
  byte[] nativeList=cat(f(6,share),f(6,f(10,f(AskNative.SECONDARY,nativeButton))),f(6,overflow));
  byte[] portrait=f(10,cat(OriginalVisualPanelTest.number(1,2),f(3,s("PAyouchat"))));
  AskNative.nativeWatchPresentation=portrait;AskNative.nativeFullscreenPresentation=null;
  ok(nativeList==AskNative.insertQuickModel(nativeList,"abcdefghijk",0),"genuine fullscreen Ask preserved");
  ok(Arrays.equals(AskNative.nativeFullscreenPresentation,eligiblePresentation),"native side-panel presentation learned");
  ok(Arrays.equals(AskNative.nativeWatchPresentation,portrait),"portrait cache not replaced by fullscreen");
  byte[] originalVisual=AskNative.genuineFullscreenVisual;
  AskNative.captureFullscreen(f(4,f(169495254,f(138681778,eligiblePresentation))));
  ok(Arrays.equals(originalVisual,AskNative.genuineFullscreenVisual),"unsupported icon never replaces valid native visual with empty cache");
  // The native bridge is reused, no Android overlay/new executor API.
  AskFlyoutMenuPatch.CommandExecutorAccess executor=b->{};
  AskNative.route(new RouteTest.Message(marker),executor);
  ok(!AskFlyoutMenuPatch.autoSummary(),"fullscreen direct opening does not auto summarize");
  byte[] generated=f(138681778,cat(f(2,f(138681066,f(90,s("CURRENT_CHAT")))),f(10,cat(OriginalVisualPanelTest.number(1,2),f(3,s("PAyouchat"))))));
  byte[] adapted=AskNative.adaptPanelCommand(generated);
  ok(AskNative.text(get(get(adapted,138681778),1)).equals("PAyouchat"),"fullscreen opening picks fullscreen layout");
  ok(get(get(adapted,138681778),10).length==0,"portrait target not used for side panel");
  AskFlyoutMenuPatch.showAskPanel("abcdefghijk");
  ok(AskNative.adaptPanelCommand(generated)==generated,"normal official menu preserved after fullscreen");
  System.out.println("PASS fullscreen typed Secondary list insertion, order, icon, current video, native capture, separate presentation cache, direct native route, unknown/malformed schemas preserved (synthetic JVM fixtures; no Android UI proof)");
 }
}
