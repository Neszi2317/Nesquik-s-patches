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

import static app.neszi.reddit.customhome.CustomHome.*;

@SuppressWarnings("deprecation")
public final class CustomHomeSettings {
    public static void addSettings(PreferenceFragment fragment) {
        PreferenceScreen root = fragment.getPreferenceScreen();
        if (root == null || root.findPreference("neszi_custom_home_screen") != null) return;
        Context ctx = fragment.getActivity();
        PreferenceScreen page = fragment.getPreferenceManager().createPreferenceScreen(ctx);
        page.setKey("neszi_custom_home_screen"); page.setTitle("Custom Home");
        page.setSummary("Home subreddit and main feed blocking"); root.addPreference(page);
        SwitchPreference enable = new SwitchPreference(ctx);
        enable.setTitle("Enable custom home"); enable.setPersistent(false); enable.setChecked(enabled());
        enable.setOnPreferenceChangeListener((p, value) -> prefs().edit().putBoolean(ENABLED, (Boolean) value).commit());
        page.addPreference(enable);
        EditTextPreference sub = new EditTextPreference(ctx);
        sub.setTitle("Default subreddit"); sub.setDialogTitle("Default subreddit");
        sub.setPersistent(false); sub.setText(subreddit()); sub.setSummary("r/" + subreddit());
        sub.setOnPreferenceChangeListener((p, value) -> {
            try {
                String normalized = SubredditName.normalize(String.valueOf(value));
                if (!prefs().edit().putString(SUB, normalized).commit()) return false;
                sub.setText(normalized); sub.setSummary("r/" + normalized);
                return false; // already set normalized text, don't overwrite with raw input
            } catch (IllegalArgumentException e) { Toast.makeText(ctx, e.getMessage(), Toast.LENGTH_LONG).show(); return false; }
        }); page.addPreference(sub);
        SwitchPreference block = new SwitchPreference(ctx);
        block.setTitle("Disable main feed"); block.setSummary("Redirect Home feed screens before rendering");
        block.setPersistent(false); block.setChecked(prefs().getBoolean(BLOCK, true));
        block.setOnPreferenceChangeListener((p, value) -> prefs().edit().putBoolean(BLOCK, (Boolean) value).commit());
        page.addPreference(block);
        Preference apply = new Preference(ctx);
        apply.setTitle("Apply changes"); apply.setSummary("Reopen Reddit with a fresh navigation stack");
        apply.setOnPreferenceClickListener(p -> {
            Activity activity = fragment.getActivity();
            Intent launch = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
            if (launch == null) { Toast.makeText(ctx, "Close and reopen Reddit", Toast.LENGTH_LONG).show(); return true; }
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(launch); return true;
        }); page.addPreference(apply);
    }
    private CustomHomeSettings() {}
}
