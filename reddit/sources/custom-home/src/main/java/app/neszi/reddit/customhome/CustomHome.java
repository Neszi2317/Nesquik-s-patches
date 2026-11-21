package app.neszi.reddit.customhome;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.widget.Toast;
import com.reddit.navstack.v;
import com.reddit.screens.listing.compose.SubredditFeedScreen;

/** Runtime extension; its unique package and preferences do not replace Morphe classes. */
@SuppressWarnings("deprecation")
public final class CustomHome {
    private static volatile Context context;
    private static boolean pendingInitialHome = true;
    private static final String FILE = "neszi_custom_home_v1";
    static final String ENABLED = "enabled", BLOCK = "disable_main_feed", SUB = "subreddit";
    private static final String HOME = "com.reddit.feeds.home.impl.ui.HomeFeedScreen";
    private static final String SCREEN_KEY = "neszi.custom_home.subreddit";
    public static void init(Context app) { context = app.getApplicationContext(); }
    public static void beginActivity(Context activity) {
        if (context == null) init(activity);
        pendingInitialHome = true;
    }
    public static Bundle prepareHomeArgs(Bundle original) {
        if (!enabled()) return original;
        Bundle args = original == null ? new Bundle() : new Bundle(original);
        args.putInt("currentTabIndex", 0);
        return args;
    }
    static SharedPreferences prefs() {
        Context c = context;
        if (c == null) throw new IllegalStateException("Custom Home context not initialized");
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
    public static boolean enabled() { return prefs().getBoolean(ENABLED, true); }
    public static boolean blocking() { return enabled() && prefs().getBoolean(BLOCK, true); }
    public static boolean hideHome(Enum<?> tab) { return enabled() && tab != null && "Home".equals(tab.name()); }
    public static String subreddit() {
        try { return SubredditName.normalize(prefs().getString(SUB, "nightfeeling")); }
        catch (IllegalArgumentException e) { return "nightfeeling"; }
    }
    /** Factory boundary, after Reddit's Home-only analytics but before lifecycle/render. */
    public static Object replaceFactoryResult(Object screen) {
        if (!enabled() || screen == null || !HOME.equals(screen.getClass().getName())) return screen;
        if (!blocking() && !pendingInitialHome) return screen;
        pendingInitialHome = false;
        return replace(screen);
    }
    /** NavEntry construction catches fresh and restored modern-stack Home screens. */
    public static v replaceEntry(v screen) {
        return blocking() ? (v) replace(screen) : screen;
    }
    private static Object replace(Object object) {
        if (object == null || !HOME.equals(object.getClass().getName())) return object;
        v original = (v) object;
        Bundle args = original.c == null ? new Bundle() : new Bundle(original.c);
        // Never restore Home's saved view model / child-router state into a subreddit.
        args.remove("Screen.STATE_INSTANCE_PROPERTIES");
        args.remove("Screen.WILL_RESTORE");
        args.putString("subredditName", subreddit());
        args.putString(SCREEN_KEY, subreddit());
        return new SubredditFeedScreen(args);
    }
    private CustomHome() {}
}
