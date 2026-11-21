package app.morphe.extension.shared;
import android.content.*;import java.io.*;import java.lang.reflect.*;import java.nio.file.*;import java.util.*;
/** Host adapter for disk-backed SharedPreferences; exercises production save/load code. */
public class Utils {
 public static Path file;public static Context getContext(){return new ContextWrapper(null){@Override public SharedPreferences getSharedPreferences(String name,int mode){
  Properties data=new Properties();try{if(Files.exists(file))try(InputStream in=Files.newInputStream(file)){data.load(in);}}catch(IOException e){throw new RuntimeException(e);}
  InvocationHandler h=(proxy,m,args)->{switch(m.getName()){
   case "getString":return data.getProperty((String)args[0],(String)args[1]);
   case "edit":return Proxy.newProxyInstance(Utils.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},(ed,em,ea)->{switch(em.getName()){
    case "putString":data.setProperty((String)ea[0],(String)ea[1]);return ed;
    case "commit":try(OutputStream out=Files.newOutputStream(file)){data.store(out,"test preferences");}return true;
    default:throw new UnsupportedOperationException(em.getName());}});
   default:throw new UnsupportedOperationException(m.getName());}};
  return (SharedPreferences)Proxy.newProxyInstance(Utils.class.getClassLoader(),new Class[]{SharedPreferences.class},h);
 }};}
}
