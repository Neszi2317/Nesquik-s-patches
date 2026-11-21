package fr.nesquik.hidden;
import android.content.SharedPreferences;
import app.morphe.extension.shared.Utils;
import androidx.compose.runtime.i;
import androidx.compose.runtime.w1;
import java.util.*;

final class Store {
    static final java.util.concurrent.ExecutorService IO=java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final w1 revision = i.s(0);
    private static final class Data {
        static final SharedPreferences db=Utils.getContext().getSharedPreferences("fr.nesquik.hidden.v1",0);
        static volatile Set<String> ids=Collections.unmodifiableSet(new HashSet<>(db.getStringSet("ids",Collections.emptySet())));
        static volatile boolean enabled=db.getBoolean("enabled",true);
    }
    static void observe(){revision.getValue();}
    static Set<String> ids(){return Data.ids;}
    static boolean enabled(){return Data.enabled;}
    private static void changed(){revision.setValue(new Object());}
    static synchronized void enabled(boolean on){
        if(!Data.db.edit().putBoolean("enabled",on).commit()) throw new IllegalStateException("Écriture impossible");
        Data.enabled=on; changed();
    }
    static synchronized void edit(Collection<String> add,String remove,boolean clear){
        Set<String> next=clear?new HashSet<>():new HashSet<>(Data.ids);
        next.addAll(add);
        if(remove!=null) next.remove(remove);
        if(next.size()>Ids.MAX) throw new IllegalArgumentException("Trop de posts");
        if(next.equals(Data.ids)) return;
        if(!Data.db.edit().putStringSet("ids",next).commit()) throw new IllegalStateException("Écriture impossible");
        Data.ids=Collections.unmodifiableSet(next); changed();
    }
}
