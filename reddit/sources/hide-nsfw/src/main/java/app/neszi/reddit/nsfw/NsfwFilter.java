package app.neszi.reddit.nsfw;

import com.reddit.domain.model.Link;
import java.util.*;

/** Always-on local filtering; no network, title matching, preference changes or UI hook. */
public final class NsfwFilter {
    public interface Sources { Object[] neszi_nsfwSources(); }
    private static final Set<String> MARKER_ENUMS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "com.reddit.feeds.model.IndicatorType", "com.reddit.type.CellIndicatorType")));

    public static List<?> filter(List<?> input) {
        if (input == null || input.isEmpty()) return input;
        List<Object> kept = null;
        int index = 0;
        for (Object item : input) {
            boolean remove = isNsfw(item);
            if (remove && kept == null) {
                kept = new ArrayList<>(input.size());
                kept.addAll(input.subList(0, index));
            } else if (!remove && kept != null) kept.add(item);
            index++;
        }
        return kept == null ? input : kept;
    }
    public static boolean isNsfw(Object item) {
        return scan(item, Collections.newSetFromMap(new IdentityHashMap<>()));
    }
    private static boolean scan(Object item, Set<Object> visited) {
        if (item == null || !visited.add(item)) return false;
        if (item instanceof Link && ((Link)item).getOver18()) return true;
        if (item instanceof Enum<?>) {
            Enum<?> marker = (Enum<?>)item;
            return "NSFW".equals(marker.name()) && MARKER_ENUMS.contains(marker.getDeclaringClass().getName());
        }
        if (item instanceof Sources) {
            Object[] children = ((Sources)item).neszi_nsfwSources();
            if (children != null) for (Object child : children) if (scan(child, visited)) return true;
        } else if (item instanceof Iterable<?>) {
            for (Object child : (Iterable<?>)item) if (scan(child, visited)) return true;
        } else if (item instanceof Object[]) {
            for (Object child : (Object[])item) if (scan(child, visited)) return true;
        }
        return false;
    }
    private NsfwFilter() {}
}
