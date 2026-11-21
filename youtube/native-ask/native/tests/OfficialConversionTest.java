package app.neszi.asknative;
import java.util.*;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.youtube.innertube.FeedCardLayoutOuterClass.*;
public class OfficialConversionTest {
 static byte[] convert(byte[] input,OfficialLayoutHarness.VideoActionBarLayout layout) {
  OfficialLayoutHarness.LAYOUT=layout;
  AskNative.beforeActionBarLayout(input);
  return AskNative.afterActionBarLayout(OfficialLayoutHarness.setVideoActionBarLayout(input));
 }
 static void ok(boolean b,String m){NativeProtoTest.check(b,m);}
 public static void main(String[]args)throws Exception {
  Utils.CONTEXT.resources.config.smallestScreenWidthDp=400;
  // Use generated official protobuf types; real unknown-field retention and real layout converter.
  String video="abcdefghijk";
  var like=LikeButtonViewModel.newBuilder().setLikeStatusEntityKey(java.util.Base64.getUrlEncoder().encodeToString(LikeStatusEntityKey.newBuilder().setVideoId(video).build().toByteArray())).build();
  var dislike=DislikeButtonViewModel.newBuilder().build();
  var rating=VideoActionBarLikeDislike.newBuilder().setLikeButton(VideoActionBarItemContent.newBuilder().setLikeButton(like)).setDislikeButton(VideoActionBarItemContent.newBuilder().setDislikeButton(dislike)).build();
  var share=ButtonViewModel.newBuilder().setIconName("yt_outline_share").setTargetId("id.video.share.button").setType(5).setButtonSize(2).build();
  var save=AddToPlaylistButtonViewModel.newBuilder().build();
  var nativeAsk=ButtonViewModel.newBuilder().setIconName("yt_fill_spark").setTitle("Posez une question").setType(5).setButtonSize(2).build();
  var bar=VideoActionBarViewModel.newBuilder().setLayoutStyle(4)
    .addActions(VideoActionBarItem.newBuilder().setContent(VideoActionBarItemContent.newBuilder().setLikeDislikeButtons(rating)))
    .addActions(VideoActionBarItem.newBuilder().setContent(VideoActionBarItemContent.newBuilder().setButton(share)))
    .addActions(VideoActionBarItem.newBuilder().setContent(VideoActionBarItemContent.newBuilder().setSaveButton(save)))
    .addActions(VideoActionBarItem.newBuilder().setContent(VideoActionBarItemContent.newBuilder().setButton(nativeAsk))).build();
  byte[] raw=MinimalLayoutTest.element(bar.toByteArray());
  byte[] bound=MinimalLayoutTest.elementFromRoot(AskNative.transformNextBytes(MinimalLayoutTest.root(raw,video)));
  byte[] minimal=convert(bound,OfficialLayoutHarness.VideoActionBarLayout.MINIMAL);
  ok(AskNative.text(AskNative.first(minimal,AskNative.ELEMENT_VIDEO)).equals(video),"real official protobuf keeps response-video unknown field through toBuilder conversion");
  var output=VideoActionBarViewModel.parseFrom(MinimalLayoutTest.data(minimal));
  ok(output.getLayoutStyle()==6&&output.getActionsCount()==5,"official classic->minimal conversion retains native 5-slot capacity");
  ok(output.getActions(3).getContent().getButton().getIconName().equals("yt_fill_spark"),"after official conversion Ask is visible after Share");
  var overflow=output.getActions(4).getContent().getOverflow();
  ok(overflow.getMenuItemsList().stream().anyMatch(VideoActionBarItemContent::hasSaveButton),"Save remains in native menu");
  ok(overflow.getMenuItemsList().stream().noneMatch(x->x.hasButton()&&x.getButton().getIconName().contains("spark")),"Ask not duplicated in overflow");
  byte[] classic=convert(minimal,OfficialLayoutHarness.VideoActionBarLayout.CLASSIC);
  var classicBar=VideoActionBarViewModel.parseFrom(MinimalLayoutTest.data(classic));
  ok(classicBar.getLayoutStyle()==4,"switch back to classic works");
  ok(classicBar.getActionsList().stream().anyMatch(x->x.getContent().hasButton()&&x.getContent().getButton().getIconName().contains("spark")),"Ask retained on return to classic");
  ok(classicBar.getActionsList().stream().anyMatch(x->x.getContent().hasSaveButton()),"Save retained on return to classic");
  for(var action:classicBar.getActionsList())if(action.getContent().hasButton())ok(action.getContent().getButton().getIconStyle()==0,"no minimal icon style leaks to classic");
  byte[] again=convert(classic,OfficialLayoutHarness.VideoActionBarLayout.MINIMAL);
  ok(VideoActionBarViewModel.parseFrom(MinimalLayoutTest.data(again)).getActionsCount()==5,"second layout switch remains stable");
  System.out.println("PASS exact official converter methods + generated official protobuf: classic/minimal/classic round trip, video binding retained, Ask visible, Save preserved, styles isolated (JVM, not Android UI)");
 }
}
