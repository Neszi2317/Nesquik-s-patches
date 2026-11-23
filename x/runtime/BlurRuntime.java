package app.morphe.extension.newx.mediablur;
import android.content.Context;import android.view.MotionEvent;import android.view.ViewConfiguration;
import java.util.*;import kotlin.jvm.functions.*;
import static app.morphe.extension.newx.mediablur.Reflector.*;
/** ABI pinned to the inspected X 12.43.0-prod.01. Uses the app's own Compose runtime. */
public final class BlurRuntime {
 private static final Map<Object,Session> SESSIONS=Collections.synchronizedMap(new WeakHashMap<Object,Session>());
 private static final Map<Object,Object> ORIGINALS=Collections.synchronizedMap(new WeakHashMap<Object,Object>());
 private static Object local;
 private static Object rowLocal;
 private static synchronized Object rowLocal(){if(rowLocal==null)rowLocal=make("androidx.compose.runtime.g0",new Function0<Object>(){public Object invoke(){return null;}},(byte)1);return rowLocal;}
 private static final class Row {
  final boolean mediaAllowed;
  final boolean root;
  Row(boolean mediaAllowed, boolean root) {
   this.mediaAllowed=mediaAllowed;
   this.root=root;
  }
 }
 private static Row row(Object composer){return (Row)call(composer,"m",rowLocal());}
 private static boolean mediaAllowed(Object composer){Row row=row(composer);return row!=null&&row.mediaAllowed;}
 private static boolean rootHeader(Object composer){Row row=row(composer);return row!=null&&row.root;}
 private static Object unit(){return field(cls("kotlin.Unit"),"a");}
 private static synchronized Object local(){if(local==null)local=make("androidx.compose.runtime.g0",new Function0<Object>(){public Object invoke(){return null;}},(byte)1);return local;}
 private static Context context(){Context c=(Context)stat("app.morphe.extension.shared.Utils","getContext");BlurPreferences.init(c);return c;}
 private static Session session(Object composer){return (Session)call(composer,"m",local());}
 private static final class Session {
  final SessionState state=new SessionState();
  final long rootId;
  Session(Object controller){rootId=PostIdentity.rootPostId(controller);}
  final Object revision=stat("androidx.compose.runtime.i","s",Integer.valueOf(0));
  void observe(){call(revision,"getValue");}
  void changed(){int n=((Number)call(revision,"getValue")).intValue();call(revision,"setValue",n+1);}
 }
 public static void scope(final Object[] args,final int which){
  final int composerIndex;
  final String owner;
  final String method;
  switch(which){
   case 0:
    composerIndex=4;owner="com.x.postdetail.c";method="pikoBlurOriginal_d";break;
   case 1:
    composerIndex=4;owner="com.x.postdetailsheet.a";method="pikoBlurOriginal_a";break;
   case 2:
    composerIndex=5;owner="com.x.sensitivemedia.api.c";method="pikoBlurOriginal_b";break;
   case 3:
    composerIndex=3;owner="com.x.urt.items.post.g";method="pikoBlurOriginal_a";break;
   default:
    throw new IllegalArgumentException("Unknown media blur scope: "+which);
  }
  final Object composer=args[composerIndex];
  final Object value;
  if(which==2||which==3){
   Session current=session(composer);
   if(current==null){stat(owner,method,args);return;}
   Row inherited=row(composer);
   if(which==3&&inherited!=null){stat(owner,method,args);return;}
   long id=PostIdentity.rowPostId(args[0]);
   boolean allowed=PostIdentity.mediaAllowed(inherited==null?null:inherited.mediaAllowed,id,current.rootId);
   value=call(rowLocal(),"a",new Row(allowed,which==2&&inherited==null&&id==current.rootId));
  }else{
   Object model=which==0?args[1]:field(args[0],"f");
   Session current=SESSIONS.get(model);if(current==null){current=new Session(model);SESSIONS.put(model,current);}
   value=call(local(),"a",current);
  }
  Function2<Object,Object,Object> content=new Function2<Object,Object,Object>(){public Object invoke(Object c,Object flags){
   Object[] next=args.clone();next[composerIndex]=c;next[composerIndex+1]=((Integer)next[composerIndex+1])|1;
   stat(owner,method,next);return unit();
  }};
  stat("androidx.compose.runtime.i","c",value,content,composer,0);
 }
 /** Null return means: do not alter any caller parameter, flags, or default masks. */
 public static Object media(Object model,Object modifier,Object composer){
  Session s=session(composer);if(s==null||!mediaAllowed(composer))return null;context();s.observe();
  Object base=modifier==null?field(cls("androidx.compose.ui.q"),"a"):modifier;
  Object previous=ORIGINALS.get(base);if(previous!=null)base=previous;
  if(!BlurPreferences.enabled())return base;
  String key=(String)call(model,"a");
  if(key==null||key.length()==0)throw new IllegalStateException("Missing native media identity");
  if(!s.state.hidden(key))return base;
  Object blurred=stat("androidx.compose.ui.draw.j","b",base,BlurPreferences.radius(),BlurPreferences.radius(),field(cls("androidx.compose.ui.graphics.n0"),"b"));
  if(BlurPreferences.reveal()){
   Object interop=make("androidx.compose.ui.input.pointer.z");
   field(interop,"a",new RevealTouch(s,key));
   // Ancestor pointer interop consumes the reveal gesture before descendant click handlers.
   blurred=call(blurred,"p0",interop);
  }
  ORIGINALS.put(blurred,base);return blurred;
 }
 private static final class RevealTouch implements Function1<Object,Boolean>{
  final Session s;final String key;float x,y;boolean down,moved;final int slop;
  RevealTouch(Session s,String key){this.s=s;this.key=key;slop=ViewConfiguration.get(context()).getScaledTouchSlop();}
  public Boolean invoke(Object event){MotionEvent e=(MotionEvent)event;switch(e.getActionMasked()){
   case MotionEvent.ACTION_DOWN:down=true;moved=false;x=e.getX();y=e.getY();return true;
   case MotionEvent.ACTION_MOVE:if(Math.abs(e.getX()-x)>slop||Math.abs(e.getY()-y)>slop)moved=true;return down;
   case MotionEvent.ACTION_UP:if(down&&!moved&&e.getPointerCount()==1){s.state.reveal(key);s.changed();}down=false;return true;
   case MotionEvent.ACTION_POINTER_DOWN:moved=true;return down;
   case MotionEvent.ACTION_CANCEL:down=false;return true;
   default:return down;
  }}
 }
 /** Keep the surface type stable throughout this screen, including while blur is off. */
 public static int surface(int original,Object composer){if(session(composer)==null||!mediaAllowed(composer))return original;context();return BlurPreferences.enabled()?2:original;}
 public static Object click(Object original,Object composer){if(original instanceof ToggleClick)original=((ToggleClick)original).original;final Session s=session(composer);if(s==null||!rootHeader(composer))return original;context();s.observe();if(!BlurPreferences.enabled())return original;return new ToggleClick(s,original);}
 private static final class ToggleClick implements Function0<Object>{final Session s;final Object original;ToggleClick(Session s,Object o){this.s=s;original=o;}public Object invoke(){s.state.toggle();s.changed();return unit();}}
 public static Object icon(Object original,Object composer){Session s=session(composer);if(s==null||!rootHeader(composer))return original;Context c=context();s.observe();if(!BlurPreferences.enabled())return original;int id=c.getResources().getIdentifier(s.state.active()?"ic_piko_blur_on":"ic_piko_blur_off","drawable",c.getPackageName());if(id==0)throw new IllegalStateException("Missing In-App Media Blur drawable");return make("com.x.icons.b",id,false);}
 private static boolean enabledHere(Object composer){if(session(composer)==null||!rootHeader(composer))return false;context();return BlurPreferences.enabled();}
 public static boolean headerVisible(boolean original,Object composer){return enabledHere(composer)||original;}
 public static String label(String original,Object composer){Session s=session(composer);if(s==null||!rootHeader(composer)||!BlurPreferences.enabled())return original;s.observe();return s.state.active()?"Désactiver le flou des médias":"Activer le flou des médias";}
}
