import java.lang.reflect.Method;
import java.util.Collection;

public class ManagerProbe {
    public static void main(String[] args) throws Throwable {
        Class<?> patchBundle = Class.forName("app.morphe.manager.patcher.patch.PatchBundle");
        Object bundle = patchBundle.getConstructor(String.class).newInstance(args[0]);
        Class<?> loader = Class.forName(patchBundle.getName() + "$Loader");
        Object instance = loader.getField("INSTANCE").get(null);
        Method patches = loader.getMethod("patches", patchBundle);
        Collection<?> list = (Collection<?>) patches.invoke(instance, bundle);
        for (Object patch : list) {
            Object name = patch.getClass().getMethod("getName").invoke(patch);
            if ("YouTube: Playlist Search & Date Filter".equals(name)) {
                System.out.println("Manager's Android DEX loader found: " + name + " (bundle entries: " + list.size() + ")");
                return;
            }
        }
        throw new AssertionError("Manager patch name absent");
    }
}
