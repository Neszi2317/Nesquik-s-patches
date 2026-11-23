package app.morphe.extension.shared;

import android.content.Context;
import android.content.SharedPreferences;
import java.lang.reflect.Proxy;

public final class Utils {
    private static final SharedPreferences SETTINGS = (SharedPreferences) Proxy.newProxyInstance(
        SharedPreferences.class.getClassLoader(), new Class<?>[]{SharedPreferences.class},
        (proxy, method, args) -> switch (method.getName()) {
            case "getBoolean" -> !"reveal".equals(args[0]);
            case "getString" -> args[1];
            default -> throw new UnsupportedOperationException(method.getName());
        });

    private static final Context CONTEXT = new Context() {
        @Override public SharedPreferences getSharedPreferences(String name, int mode) { return SETTINGS; }
    };

    public static Context getContext() { return CONTEXT; }
}
