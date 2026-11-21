package fr.nesquik.hidden;
import android.widget.Toast;
import app.morphe.extension.shared.Utils;
import com.x.models.timelines.items.k1;
import java.lang.reflect.*;
import java.util.*;

public final class Menu {
    // Set inside the native enum initializer. This instance is private to our menu rows.
    public static Object action;
    public static List<?> add(List<?> groups){
        if(groups==null || groups.isEmpty() || action==null || !Store.enabled()) return groups;
        try {
            Object exemplar=groups.get(0);
            if(exemplar==null)return groups;
            Constructor<?> ctor=exemplar.getClass().getDeclaredConstructor(List.class);
            ctor.setAccessible(true);
            for(Object group:groups){
                if(group==null)continue;
                for(Field f:group.getClass().getDeclaredFields()){
                    if(!List.class.isAssignableFrom(f.getType()) || Modifier.isStatic(f.getModifiers()))continue;
                    f.setAccessible(true);
                    Object list=f.get(group);
                    if(list instanceof List && ((List<?>)list).contains(action))return groups;
                }
            }
            List<Object> result=new ArrayList<>(groups);
            result.add(ctor.newInstance(Collections.singletonList(action)));
            return result;
        } catch(ReflectiveOperationException | RuntimeException e){return groups;}
    }
    public static Object label(Object value,Object original){
        return value!=null && value==action?"Masquer définitivement ce post":original;
    }
    public static Object icon(Object value){
        if(value==null || value!=action)return value;
        Enum<?> e=(Enum<?>)value;
        return e.getDeclaringClass().getEnumConstants()[e.ordinal()];
    }
    public static boolean handle(Object presenter,Object value){
        if(value==null || value!=action)return false;
        try {
            k1 selected=null;
            for(Class<?> type=presenter.getClass();type!=null;type=type.getSuperclass()){
                for(Field f:type.getDeclaredFields()){
                    if(f.getType()!=k1.class || Modifier.isStatic(f.getModifiers()))continue;
                    f.setAccessible(true);
                    k1 candidate=(k1)f.get(presenter);
                    if(selected!=null && candidate!=null && selected!=candidate)throw new IllegalStateException();
                    if(candidate!=null)selected=candidate;
                }
            }
            String id=selected==null?null:String.valueOf(selected.getId());
            if(!Ids.valid(id))throw new IllegalStateException();
            Store.IO.execute(()->{
                try { Store.edit(Collections.singleton(id),null,false);message("Post masqué"); }
                catch(RuntimeException e){message("Enregistrement impossible");}
            });
        } catch(ReflectiveOperationException | RuntimeException e){message("Impossible de masquer ce post");}
        return true;
    }
    private static void message(String text){new android.os.Handler(android.os.Looper.getMainLooper()).post(()->
        Toast.makeText(Utils.getContext(),text,Toast.LENGTH_SHORT).show());}
}
