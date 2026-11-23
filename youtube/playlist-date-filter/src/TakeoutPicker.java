package fr.nesquik.playlistdate;

import static fr.nesquik.playlistdate.PlaylistDateDialog.phrase;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.innertube.utils.AuthUtils;
import app.morphe.extension.youtube.patches.utils.requests.WatchLaterEntries;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@SuppressWarnings("deprecation")
public final class TakeoutPicker extends Fragment {
    private static final int READ_CSV = 1;
    private static final String TAG = "nesquik.watch-later-takeout";

    public static void open(Activity activity) {
        var manager = activity.getFragmentManager();
        var picker = (TakeoutPicker) manager.findFragmentByTag(TAG);
        try {
            if (picker == null) {
                picker = new TakeoutPicker();
                manager.beginTransaction().add(picker, TAG).commitNow();
            }
            var intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            picker.startActivityForResult(intent, READ_CSV);
        } catch (RuntimeException ex) {
            showError(activity);
        }
    }

    @Override
    public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        Activity activity = getActivity();
        if (request != READ_CSV || result != Activity.RESULT_OK || data == null || activity == null) return;
        Uri source = data.getData();
        if (source == null) return;
        var context = activity.getApplicationContext();
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        if (AuthUtils.isNotLoggedIn()) {
            toast(context, phrase(locale, "Connecte-toi au compte YouTube de ce fichier Takeout.",
                    "Sign in to the YouTube account for this Takeout file."));
            return;
        }
        var headers = AuthUtils.getRequestHeader();
        toast(context, phrase(locale, "Lecture du CSV et de la playlist…", "Reading CSV and playlist…"));
        Utils.runOnBackgroundThread(() -> {
            try (var stream = context.getContentResolver().openInputStream(source)) {
                if (stream == null) throw new IllegalStateException("Missing document stream");
                var dates = TakeoutDates.read(new InputStreamReader(stream, StandardCharsets.UTF_8));
                var entries = WatchLaterEntries.fetch(headers);
                if (!headers.equals(AuthUtils.getRequestHeader()) || AuthUtils.isNotLoggedIn()) {
                    throw new IllegalStateException("The active YouTube session changed during the import");
                }
                int matched = WatchLaterDates.open(context).importDates(dates, entries);
                String message = matched == 0
                        ? phrase(locale, "Aucune vidéo du fichier ne correspond à cette playlist.",
                                "No videos in this file matched this playlist.")
                        : String.format(locale, phrase(locale, "%d dates associées à la playlist.",
                                "%d dates matched to the playlist."), matched);
                Utils.runOnMainThreadNowOrLater(() -> toast(context, message));
            } catch (Exception ex) {
                Logger.printInfo(() -> "Watch later Takeout import failed", ex);
                Utils.runOnMainThreadNowOrLater(() -> showError(context));
            }
        });
    }

    private static void showError(Context context) {
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        toast(context, phrase(locale,
                "Import impossible. Choisis le CSV « Watch later-videos » du compte utilisé.",
                "Could not import. Choose the Watch later-videos CSV from the active account."));
    }

    private static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
