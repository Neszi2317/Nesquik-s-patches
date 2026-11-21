package app.morphe.extension.newx.anonymous;
import android.content.Context;
import android.content.SharedPreferences;
import java.security.SecureRandom;
import app.morphe.extension.shared.Utils;
public final class Names {
 public static final String PREFS="piko_anonymous_names_v1";
 private static volatile Config config;
 private static volatile AliasEngine engine;
 private static SharedPreferences preferences;
 public static final class Config {
  public final boolean enabled,names,handles;
  Config(boolean e,boolean n,boolean h){enabled=e;names=n;handles=h;}
 }
 private static final SharedPreferences.OnSharedPreferenceChangeListener LISTENER=(p,k)->reload(p);
 private static void reload(SharedPreferences p){config=new Config(p.getBoolean("enabled",true),p.getBoolean("names",true),p.getBoolean("handles",true));}
 public static synchronized boolean initialize(){
  if(engine!=null)return true;
  Context context=Utils.getContext();if(context==null)return false;
  SharedPreferences p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
  String saved=p.getString("secret",null);byte[] key=new byte[32];
  if(saved==null){new SecureRandom().nextBytes(key);if(!p.edit().putString("secret",AliasEngine.hex(key,0,key.length)).commit())return false;}
  else{if(saved.length()!=64)return false;try{for(int i=0;i<32;i++)key[i]=(byte)Integer.parseInt(saved.substring(i*2,i*2+2),16);}catch(NumberFormatException e){return false;}}
  AliasEngine ready=new AliasEngine(key);reload(p);preferences=p;p.registerOnSharedPreferenceChangeListener(LISTENER);engine=ready;return true;
 }
 public static Config config(){if(engine==null&&!initialize())return new Config(false,true,true);return config;}
 public static void set(String key,boolean value){if(!initialize())throw new IllegalStateException("Préférences indisponibles");if(!key.equals("enabled")&&!key.equals("names")&&!key.equals("handles"))throw new IllegalArgumentException(key);preferences.edit().putBoolean(key,value).apply();reload(preferences);}
 /** Pure presentation input; original user object is never changed. */
 public static String render(String original,Object user,int kind){
  if(engine==null&&!initialize())return original;
  Config c=config;if(!c.enabled||(kind==0?!c.names:!c.handles))return original;
  AliasEngine.Alias alias=engine.get(userId(user));
  if(alias==null)return original;
  return kind==0?alias.name:(kind==1?alias.handle:"@"+alias.handle);
 }
 public static Object wrap(Object user){return user==null||user instanceof PresentationUser?user:new PresentationUser(user);}
 // Replaced by the patch with the exact interface id getter and identifier formatter.
 public static String userId(Object user){throw new IllegalStateException("Anonymous Names bridge missing");}
 public static String display(Object user){throw new IllegalStateException("Anonymous Names bridge missing");}
 public static String handle(Object user){throw new IllegalStateException("Anonymous Names bridge missing");}
}
