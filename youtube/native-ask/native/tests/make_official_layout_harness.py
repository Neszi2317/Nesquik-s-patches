from pathlib import Path
source=Path('native/research082/RestoreOldVideoActionBarPatch.java').read_text()
def method(name):
 start=source.index(name);start=source.rfind('\n',0,start)+1
 opening=source.index('{',start);depth=1;i=opening+1
 while depth:
  if source[i]=='{':depth+=1
  elif source[i]=='}':depth-=1
  i+=1
 return source[start:i]
header='''package app.neszi.asknative;
import app.morphe.extension.youtube.innertube.FeedCardLayoutOuterClass.*;
public class OfficialLayoutHarness {
 public enum VideoActionBarLayout {DEFAULT,CLASSIC,CLASSIC_TWO_ROWS,MINIMAL}
 public static VideoActionBarLayout LAYOUT=VideoActionBarLayout.MINIMAL;
 private static final String VIDEO_ACTION_BAR_TEMPLATE="compactify_video_action_bar.eml",SHARE_BUTTON_ID="id.video.share.button",OVERFLOW_BUTTON_ICON="yt_outline_experimental_overflow_horizontal_black_24",MORE_ACTIONS_TEXT="accessibility_more_actions_video";
 private static final int LAYOUT_STYLE_CLASSIC=4,LAYOUT_STYLE_MINIMAL=6,CLASSIC_BUTTON_TYPE=5,CLASSIC_BUTTON_SIZE=2,CLASSIC_DOWNLOAD_BUTTON_STYLE=4,MINIMAL_BUTTON_TYPE=3,MINIMAL_BUTTON_SIZE=4,MINIMAL_BUTTON_ICON_STYLE=1,LIKE_COUNT_ENTITY_TYPE=445;
 private static final VideoActionBarSize MINIMAL_LAYOUT_SIZE=VideoActionBarSize.newBuilder().setMinButtonsWidth(150).setMaxButtonsWidth(150).setButtonSpacing(2).setMaxVisibleButtons(5).build();
 static class FeedVideoCardsPatch {static String getElementText(byte[] b){return new String(b,java.nio.charset.StandardCharsets.ISO_8859_1);}}
 static class ResourceUtils {static int getStringIdentifier(String x){return 0;}static String getString(String x){return x;}}
 static class Logger {static void printException(java.util.function.Supplier<String> msg,Exception e){throw new AssertionError(msg.get(),e);}}
 static class Base64 {static final int URL_SAFE=8,NO_WRAP=2;static byte[] decode(String s,int f){return java.util.Base64.getUrlDecoder().decode(s);}static String encodeToString(byte[] b,int f){return java.util.Base64.getUrlEncoder().encodeToString(b);}}
'''
body='\n'.join(method(n) for n in ['public static byte[] setVideoActionBarLayout(', 'private static Element withActionBar(', 'private static ButtonViewModel layoutButton(', 'private static ToggleButtonViewModelHolder layoutToggleButton('])
p=Path('native/build/officialtest/OfficialLayoutHarness.java');p.parent.mkdir(parents=True,exist_ok=True);p.write_text(header+body+'\n}\n')
print('Extracted unchanged methods from pinned official source; only external environment mocked')
