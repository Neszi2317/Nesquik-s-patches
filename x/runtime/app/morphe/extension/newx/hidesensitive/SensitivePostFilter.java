package app.morphe.extension.newx.hidesensitive;

import java.util.*;

/** Strict pre-render pass, before Piko's best-effort filters and their cache. */
public final class SensitivePostFilter {
    private static final int MAX_DEPTH = 64;
    private static final Access LIVE = new Access();
    private SensitivePostFilter() { }
    // Patched to the persisted toggle only when the feature is selected.
    public static boolean isEnabled() { return false; }
    public static Object filter(Object items) {
        return isEnabled() ? filter(items, LIVE) : items;
    }
    static Object filter(Object items, Access access) {
        if (items == null) return null;
        try {
            if (!(items instanceof Iterable<?>)) return access.freeze(Collections.emptyList());
            List<Object> result = new ArrayList<>();
            boolean changed = false;
            Set<Object> active = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Object item : (Iterable<?>) items) {
                Object kept = visit(item, access, active, 0);
                changed |= kept != item || item == null;
                if (kept != null) result.add(kept);
            }
            return changed ? access.freeze(result) : items;
        } catch (RuntimeException failure) {
            // A strict-pass failure must not restore the original unfiltered page.
            return access.freeze(Collections.emptyList());
        }
    }
    private static Object visit(Object item, Access a, Set<Object> active, int depth) {
        if (item == null || depth > MAX_DEPTH || !active.add(item)) return null;
        try {
            if (a.post(item)) return a.sensitive(item) ? null : item;
            if (a.wrapper(item)) {
                Object old = a.unwrap(item), kept = visit(old, a, active, depth + 1);
                return kept == null ? null : kept == old ? item : a.copyWrapper(item, kept);
            }
            if (!a.module(item)) return item; // Includes pagination cursors.
            List<?> children = a.children(item);
            if (children == null) return null;
            List<Object> kept = new ArrayList<>(children.size());
            Set<Object> removedIds = new HashSet<>();
            boolean changed = false;
            for (Object child : children) {
                Object filtered = visit(child, a, active, depth + 1);
                changed |= filtered != child || child == null;
                if (filtered != null) kept.add(filtered);
                else {
                    Object post = child != null && a.wrapper(child) ? a.unwrap(child) : child;
                    if (post != null && a.post(post)) removedIds.add(a.id(post));
                }
            }
            if (kept.isEmpty()) return null; // Header, footer and container disappear together.
            return changed ? a.copyModule(item, kept, removedIds) : item;
        } catch (RuntimeException failure) {
            return null; // Drop the affected item/module on classification or copy failure.
        } finally { active.remove(item); }
    }
    public static boolean sensitiveResult(Object result, int depth) {
        if (result == null) return false;
        if (depth > MAX_DEPTH) return true;
        if (!isContextual(result)) return false; // Tombstones have no playable media.
        if (getInterstitial(result) != null || getBlurInterstitial(result) != null) return true;
        if (sensitiveCanonical(getCanonical(result))) return true;
        Object repost = getRepost(result);
        if (repost != null && (sensitiveCanonical(getRepostCanonical(repost))
                || sensitiveResult(getRepostQuote(repost), depth + 1))) return true;
        return sensitiveResult(getQuote(result), depth + 1);
    }
    private static boolean sensitiveCanonical(Object post) {
        if (post == null) return false;
        if (isPossiblySensitive(post) || Boolean.TRUE.equals(getUserSensitiveFlag(post))) return true;
        Object media = getMedia(post);
        if (media == null) return false;
        if (!(media instanceof Iterable<?>)) return true;
        for (Object item : (Iterable<?>) media) {
            Object warning = getMediaWarning(item);
            if (warning != null && warningIsSensitive(warning)) return true;
        }
        return false;
    }
    /** Tests override only the model adapter; the production traversal is exercised unchanged. */
    static class Access {
        boolean post(Object o) { return isTimelinePost(o); }
        boolean wrapper(Object o) { return isTimelineModuleItem(o); }
        boolean module(Object o) { return isTimelineModule(o); }
        boolean sensitive(Object o) { return isSensitivePost(o); }
        Object unwrap(Object o) { return getModuleItem(o); }
        Object id(Object o) { return getPostId(o); }
        List<?> children(Object o) { return getModuleInnerContent(o); }
        Object freeze(List<Object> list) { return immutableList(list); }
        Object copyWrapper(Object o, Object child) {
            return copyModuleItem(o, child, isModuleItemDispensable(o));
        }
        Object copyModule(Object o, List<Object> children, Set<Object> removedIds) {
            Object display = getModuleDisplayType(o);
            if (display != null && isVerticalConversation(display)) {
                List<?> old = getVerticalConversationPostIds(display);
                if (old != null) {
                    List<Object> ids = new ArrayList<>();
                    for (Object id : old) if (!removedIds.contains(id)) ids.add(id);
                    if (ids.size() != old.size()) display = copyVerticalConversation(display, ids);
                }
            }
            return SensitivePostFilter.copyModule(o, children, getModuleHeader(o), getModuleFooter(o),
                    display, getModuleSortIndex(o), getModuleEntryId(o), getModuleClientEventInfo(o));
        }
    }
    private static UnsupportedOperationException missing() {
        return new UnsupportedOperationException("HideSensitive bridge was not patched");
    }
    private static boolean isSensitivePost(Object o) { throw missing(); }
    private static boolean isContextual(Object o) { throw missing(); }
    private static Object getCanonical(Object o) { throw missing(); }
    private static Object getQuote(Object o) { throw missing(); }
    private static Object getRepost(Object o) { throw missing(); }
    private static Object getRepostCanonical(Object o) { throw missing(); }
    private static Object getRepostQuote(Object o) { throw missing(); }
    private static Object getInterstitial(Object o) { throw missing(); }
    private static Object getBlurInterstitial(Object o) { throw missing(); }
    private static boolean isPossiblySensitive(Object o) { throw missing(); }
    private static Object getUserSensitiveFlag(Object o) { throw missing(); }
    private static Object getMedia(Object o) { throw missing(); }
    private static Object getMediaWarning(Object o) { throw missing(); }
    private static boolean warningIsSensitive(Object o) { throw missing(); }
    private static boolean isTimelinePost(Object o) { throw missing(); }
    private static boolean isTimelineModule(Object o) { throw missing(); }
    private static boolean isTimelineModuleItem(Object o) { throw missing(); }
    private static boolean isModuleItemDispensable(Object o) { throw missing(); }
    private static Object getModuleItem(Object o) { throw missing(); }
    private static Object copyModuleItem(Object o, Object child, boolean dispensable) { throw missing(); }
    private static List<?> getModuleInnerContent(Object o) { throw missing(); }
    private static Object getModuleHeader(Object o) { throw missing(); }
    private static Object getModuleFooter(Object o) { throw missing(); }
    private static Object getModuleDisplayType(Object o) { throw missing(); }
    private static long getModuleSortIndex(Object o) { throw missing(); }
    private static String getModuleEntryId(Object o) { throw missing(); }
    private static Object getModuleClientEventInfo(Object o) { throw missing(); }
    private static Object getPostId(Object o) { throw missing(); }
    private static boolean isVerticalConversation(Object o) { throw missing(); }
    private static List<?> getVerticalConversationPostIds(Object o) { throw missing(); }
    private static Object copyVerticalConversation(Object o, List<?> ids) { throw missing(); }
    private static Object copyModule(Object o, List<?> children, Object header, Object footer,
                                    Object display, long sort, String id, Object event) { throw missing(); }
    private static Object immutableList(List<Object> list) { throw missing(); }
}
