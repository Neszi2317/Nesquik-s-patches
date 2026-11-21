package android.os;
import java.util.*;
public class Bundle {
 private final Map<String,Object> m=new HashMap<>();
 public Bundle(){} public Bundle(Bundle b){m.putAll(b.m);}
 public void putString(String k,String v){m.put(k,v);} public String getString(String k){return (String)m.get(k);}
 public void putInt(String k,int v){m.put(k,v);} public int getInt(String k){return (Integer)m.getOrDefault(k,0);}
 public void remove(String k){m.remove(k);} public boolean containsKey(String k){return m.containsKey(k);}
}
