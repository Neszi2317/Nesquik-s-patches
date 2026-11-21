package android.content;
import java.util.*;
public class Context {
 public static final int MODE_PRIVATE=0;
 private final SharedPreferences p=new MemoryPrefs();
 public Context getApplicationContext(){return this;}
 public SharedPreferences getSharedPreferences(String n,int m){return p;}
 static class MemoryPrefs implements SharedPreferences,SharedPreferences.Editor{
  Map<String,Object> m=new HashMap<>();
  public boolean getBoolean(String k,boolean d){return (Boolean)m.getOrDefault(k,d);}
  public String getString(String k,String d){return (String)m.getOrDefault(k,d);}
  public SharedPreferences.Editor edit(){return this;}
  public SharedPreferences.Editor putBoolean(String k,boolean v){m.put(k,v);return this;}
  public SharedPreferences.Editor putString(String k,String v){m.put(k,v);return this;}
  public boolean commit(){return true;}
 }
}
