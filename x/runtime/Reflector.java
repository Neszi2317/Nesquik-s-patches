package app.morphe.extension.newx.mediablur;
import java.lang.reflect.*;import java.util.concurrent.ConcurrentHashMap;
/** APK-specific reflection is pinned and checked by the patch, never guessed at runtime. */
final class Reflector {
 static final ConcurrentHashMap<String,Method> METHODS=new ConcurrentHashMap<>();
 static Class<?> cls(String n){try{return Class.forName(n);}catch(Exception e){throw new IllegalStateException(n,e);}}
 static Object field(Object obj,String n){try{Class<?> c=obj instanceof Class?(Class<?>)obj:obj.getClass();Field f=c.getField(n);return f.get(obj instanceof Class?null:obj);}catch(Exception e){throw new IllegalStateException(n,e);}}
 static void field(Object obj,String n,Object value){try{obj.getClass().getField(n).set(obj,value);}catch(Exception e){throw new IllegalStateException(n,e);}}
 static Object make(String name,Object...args){try{for(Constructor<?> c:cls(name).getConstructors())if(fits(c.getParameterTypes(),args))return c.newInstance(args);throw new NoSuchMethodException(name);}catch(Exception e){throw new IllegalStateException(name,e);}}
 static boolean fits(Class<?>[] ts,Object[] as){if(ts.length!=as.length)return false;for(int i=0;i<ts.length;i++){Class<?> t=ts[i];if(t.isPrimitive())t=t==int.class?Integer.class:t==boolean.class?Boolean.class:t==float.class?Float.class:t==long.class?Long.class:t==byte.class?Byte.class:t;if(as[i]!=null&&!t.isInstance(as[i]))return false;}return true;}
 static Object call(Object obj,String name,Object...args){Class<?> c=obj instanceof Class?(Class<?>)obj:obj.getClass();String key=c.getName()+"#"+name+"/"+args.length;Method m=METHODS.get(key);try{if(m==null||!fits(m.getParameterTypes(),args)){m=null;for(Method candidate:c.getMethods())if(candidate.getName().equals(name)&&fits(candidate.getParameterTypes(),args)){m=candidate;break;}if(m==null)throw new NoSuchMethodException(key);m.setAccessible(true);METHODS.put(key,m);}return m.invoke(obj instanceof Class?null:obj,args);}catch(InvocationTargetException e){throw new IllegalStateException(key,e.getCause());}catch(Exception e){throw new IllegalStateException(key,e);}}
 static Object stat(String c,String m,Object...a){return call(cls(c),m,a);}
}
