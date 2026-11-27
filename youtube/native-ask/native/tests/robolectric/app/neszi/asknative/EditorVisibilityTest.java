package app.neszi.asknative;
import static org.junit.Assert.*;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.view.*;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import app.morphe.extension.shared.Utils;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, manifest=Config.NONE, qualifiers="port")
public class EditorVisibilityTest {
 static class Content extends FrameLayout {
    WindowInsets ime;
    Content(Activity a) { super(a); }
    @Override public WindowInsets getRootWindowInsets(){return ime;}
 }
 private static void layout(Content content,int height) {
    int width=View.MeasureSpec.makeMeasureSpec(709, View.MeasureSpec.EXACTLY);
    content.measure(width, View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
    content.layout(0,0,709,height);
 }
 @Test public void inputAndSendRowStayAboveTheKeyboard() {
    Activity a=Robolectric.buildActivity(Activity.class).setup().get();
    Utils.activity=a;
    Content content=new Content(a);
    // No engagement_panel_wrapper. The native editor can be hosted by Elements.
    a.setContentView(content);
    a.findViewById(android.R.id.content).setId(View.NO_ID);
    content.setId(android.R.id.content);
    FrameLayout panel=new FrameLayout(a);
    content.addView(panel,new FrameLayout.LayoutParams(-1,-1));
    LinearLayout composer=new LinearLayout(a);
    EditText editor=new EditText(a);
    composer.addView(editor,new LinearLayout.LayoutParams(400,70));
    composer.addView(new ImageButton(a),new LinearLayout.LayoutParams(70,70));
    panel.addView(composer,new FrameLayout.LayoutParams(-1,70,Gravity.BOTTOM));
    int height=a.getWindowManager().getCurrentWindowMetrics().getBounds().height();
    int[] pos={0,0};content.getLocationInWindow(pos);
    int original=content.getLayoutParams().height;
    layout(content,height-pos[1]);
    int ime=height/3;
    assertTrue(pos[1]+composer.getBottom()>height-ime);
    content.ime=new WindowInsets.Builder().setInsets(WindowInsets.Type.ime(),Insets.of(0,0,0,ime)).setVisible(WindowInsets.Type.ime(),true).build();
    WatchKeyboard.open();
    content.getViewTreeObserver().dispatchOnPreDraw();
    int bounded=content.getLayoutParams().height;
    assertEquals(height-ime-pos[1], bounded);
    layout(content,bounded);
    assertEquals(height-ime,pos[1]+composer.getBottom());
    content.getViewTreeObserver().dispatchOnPreDraw();
    assertEquals(bounded,content.getLayoutParams().height);
    content.ime=new WindowInsets.Builder().setVisible(WindowInsets.Type.ime(),false).build();
    content.getViewTreeObserver().dispatchOnPreDraw();
    assertEquals(original,content.getLayoutParams().height);
    WatchKeyboard.stop();
 }
}
