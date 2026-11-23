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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

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
            var locale = activity.getResources().getConfiguration().getLocales().get(0);
            toast(activity, phrase(locale, "Impossible d’ouvrir le sélecteur de fichiers.",
                    "Could not open the file picker."));
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
        String scope = WatchLaterDates.scope(AuthUtils.getRequestHeader());
        toast(context, phrase(locale, "Lecture du CSV…", "Reading CSV…"));
        Utils.runOnBackgroundThread(() -> {
            Map<String, Instant> dates;
            try (var stream = context.getContentResolver().openInputStream(source)) {
                if (stream == null) throw new IllegalStateException("Missing document stream");
                dates = TakeoutDates.read(new InputStreamReader(stream, StandardCharsets.UTF_8));
            } catch (Exception ex) {
                reportError(context, ex, phrase(locale, "CSV Takeout illisible ou colonnes inconnues.",
                        "Takeout CSV could not be read or its columns were unrecognized."));
                return;
            }

            if (AuthUtils.isNotLoggedIn() || !scope.equals(WatchLaterDates.scope(AuthUtils.getRequestHeader()))) {
                Utils.runOnMainThreadNowOrLater(() -> toast(context,
                        phrase(locale, "Session YouTube modifiée pendant l’import. Réessaie sur la bonne chaîne.",
                                "The YouTube session changed during import. Retry on the correct channel.")));
                return;
            }
            try {
                int count = WatchLaterDates.open(context).importDates(scope, dates);
                String message = String.format(locale, phrase(locale,
                        "%d dates Takeout enregistrées sur ce téléphone.",
                        "%d Takeout dates stored on this device."), count);
                Utils.runOnMainThreadNowOrLater(() -> toast(context, message));
            } catch (RuntimeException ex) {
                reportError(context, ex, phrase(locale, "Impossible d’enregistrer les dates sur ce téléphone.",
                        "Could not store the dates on this device."));
            }
        });
    }

    private static void reportError(Context context, Exception ex, String message) {
        Logger.printInfo(() -> "Watch later Takeout import failed", ex);
        Utils.runOnMainThreadNowOrLater(() -> toast(context, message));
    }

    private static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
