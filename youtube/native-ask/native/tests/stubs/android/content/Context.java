package android.content;
import java.util.*;import android.content.res.Resources;
public class Context {
 public static final int MODE_PRIVATE=0;
 public final Resources resources=new Resources();public final Map<String,String> saved=new HashMap<>();
 public Resources getResources(){return resources;}
 public SharedPreferences getSharedPreferences(String n,int m){return new SharedPreferences(){
  public String getString(String k,String d){return saved.getOrDefault(k,d);}
  public Editor edit(){return new Editor(){public Editor putString(String k,String v){saved.put(k,v);return this;}public void apply(){}};}
 };}
}
