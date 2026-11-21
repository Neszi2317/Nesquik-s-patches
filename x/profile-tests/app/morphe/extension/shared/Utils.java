package app.morphe.extension.shared;
import android.content.*;import java.io.*;import java.lang.reflect.*;import java.nio.file.*;import java.util.*;
/** Host-only disk preferences adapter; not included in any extension. */
public class Utils {
 public static Path directory;
 public static Context getContext(){return new ContextWrapper(null){@Override public SharedPreferences getSharedPreferences(String name,int mode){
  Path file=directory.resolve(name+".properties");Properties data=new Properties();try{if(Files.exists(file))try(InputStream in=Files.newInputStream(file)){data.load(in);}}catch(IOException e){throw new RuntimeException(e);}
  InvocationHandler h=(proxy,m,args)->{switch(m.getName()){
   case "getString":return data.getProperty((String)args[0],(String)args[1]);
   case "getBoolean":return Boolean.parseBoolean(data.getProperty((String)args[0],args[1].toString()));
   case "registerOnSharedPreferenceChangeListener":case "unregisterOnSharedPreferenceChangeListener":return null;
   case "edit":return Proxy.newProxyInstance(Utils.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},(ed,em,ea)->{switch(em.getName()){
    case "putString":case "putBoolean":data.setProperty((String)ea[0],ea[1].toString());return ed;
    case "commit":case "apply":try(OutputStream out=Files.newOutputStream(file)){data.store(out,"host adapter");}return em.getName().equals("commit")?Boolean.TRUE:null;
    default:throw new UnsupportedOperationException(em.getName());}});
   default:throw new UnsupportedOperationException(m.getName());}};
  return (SharedPreferences)Proxy.newProxyInstance(Utils.class.getClassLoader(),new Class[]{SharedPreferences.class},h);
 }};}
}
