package app.morphe.extension.newx.profileimages;
import android.content.*;import android.graphics.*;import android.net.Uri;
import app.morphe.extension.shared.Utils;
/** Only presentation/load inputs are changed. Account models and URLs stored by X stay intact. */
public final class ProfileImages {
 public static final String PREFS="piko_profile_images_v1",AVATAR="piko-profile-local://avatar",BANNER="piko-profile-local://banner";
 private static volatile SharedPreferences prefs;
 private static volatile boolean avatars=true,banners=true;
 private static volatile Bitmap avatarBitmap,bannerBitmap;
 private static final Paint BLACK=new Paint(Paint.ANTI_ALIAS_FLAG);
 static { BLACK.setColor(Color.BLACK); }
 private static final SharedPreferences.OnSharedPreferenceChangeListener LISTENER=(p,k)->reload(p);
 private static void reload(SharedPreferences p){avatars=p.getBoolean("avatars",true);banners=p.getBoolean("banners",true);}
 public static synchronized void initialize(){if(prefs!=null)return;Context c=Utils.getContext();if(c==null)return;SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);reload(p);p.registerOnSharedPreferenceChangeListener(LISTENER);prefs=p;}
 public static boolean enabled(boolean avatar){if(prefs==null)initialize();return avatar?avatars:banners;}
 public static void set(String key,boolean on){initialize();if(prefs==null)throw new IllegalStateException("Settings unavailable");if(!key.equals("avatars")&&!key.equals("banners"))throw new IllegalArgumentException(key);prefs.edit().putBoolean(key,on).apply();reload(prefs);}
 public static String avatarUrl(String original){return enabled(true)?AVATAR:original;}
 public static String bannerUrl(String original){return enabled(false)?BANNER:original;}
 /** Strict host + dedicated path classification; query substrings and tweet media never match. */
 public static int urlKind(String value){
  if(value==null)return 0;if(value.equals(AVATAR))return 1;if(value.equals(BANNER))return 2;
  try {java.net.URI u=new java.net.URI(value);String host=u.getHost();String scheme=u.getScheme();if(host==null||!("https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme)))return 0;
   if(!host.equalsIgnoreCase("pbs.twimg.com")&&!host.equalsIgnoreCase("abs.twimg.com"))return 0;
   String path=u.getPath();if(path==null)return 0;
   if(path.startsWith("/profile_images/")||path.startsWith("/sticky/default_profile_images/")||path.startsWith("/default_profile_images/"))return 1;
   return path.startsWith("/profile_banners/")?2:0;
  }catch(java.net.URISyntaxException e){return 0;}
 }
 public static int neutralKind(Object o){if(o==null)return 0;if(o==avatarBitmap)return 1;if(o==bannerBitmap)return 2;return 0;}
 public static Object substitute(Object value){
  if(neutralKind(value)!=0)return value;
  String s=value instanceof String?(String)value:value instanceof Uri?value.toString():modelUrl(value);
  int kind=urlKind(s);if(kind==0||!enabled(kind==1))return value;return bitmap(kind);
 }
 private static synchronized Bitmap bitmap(int kind){
  Bitmap b=kind==1?avatarBitmap:bannerBitmap;if(b!=null)return b;
  b=Bitmap.createBitmap(kind==1?256:4,kind==1?256:4,Bitmap.Config.ARGB_8888);
  if(kind==1)new Canvas(b).drawCircle(128f,128f,128f,BLACK);else b.eraseColor(Color.BLACK);
  if(kind==1)avatarBitmap=b;else bannerBitmap=b;return b;
 }
 public static String modelUrl(Object value){throw new IllegalStateException("Profile model bridge missing");}
 public static String avatarViewerUrl(String value){return value==null?null:avatarUrl(value);}
 public static String bannerViewerUrl(String value){return value==null?null:bannerUrl(value);}
 public static boolean blockViewer(String value){int k=urlKind(value);return k!=0&&enabled(k==1);}
 public static String cacheKey(String old,Object data){int k=neutralKind(data);return k==0?old:"piko-profile-images-v1-"+k;}
 public static Object clearPlaceholder(Object old,Object data){return neutralKind(data)==0?old:null;}
 public static void draw(Canvas canvas,long size,int kind){float width=Float.intBitsToFloat((int)(size>>>32)),height=Float.intBitsToFloat((int)size);if(!(width>0&&height>0))return;if(kind==1)canvas.drawCircle(width/2,height/2,Math.min(width,height)/2,BLACK);else canvas.drawRect(0,0,width,height,BLACK);}
 // Replaced with exact typed release bridges; no reflection, class names or URI parsing per frame.
 public static boolean guard(Object model,Object scope){throw new IllegalStateException("Profile image draw bridge missing");}
}
