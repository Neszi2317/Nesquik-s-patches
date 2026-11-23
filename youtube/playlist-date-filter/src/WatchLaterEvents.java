package fr.nesquik.playlistdate;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.youtube.patches.utils.requests.PlaylistRoutes;
import java.time.Instant;
import java.util.Map;
import org.json.JSONObject;

public final class WatchLaterEvents {
    private static final ThreadLocal<String> morpheWatchLaterVideo = new ThreadLocal<>();

    private WatchLaterEvents() {}

    /** Replaces the body construction call in Morphe's direct save requests. */
    public static byte[] morpheAddBody(String video, String playlist, String removedEntry) {
        if (WatchLaterDates.PLAYLIST.equals(playlist) && (removedEntry == null || removedEntry.isEmpty())) {
            morpheWatchLaterVideo.set(video);
        } else {
            morpheWatchLaterVideo.remove();
        }
        return PlaylistRoutes.editPlaylistBody(video, playlist, removedEntry);
    }

    public static void morpheResponse(JSONObject response) {
        String video = morpheWatchLaterVideo.get();
        morpheWatchLaterVideo.remove();
        if (video == null) return;
        try {
            String entry = WatchLaterConfirmation.fromJson(video, response);
            if (entry != null) record(Map.of(video, entry));
        } catch (RuntimeException ex) {
            Logger.printInfo(() -> "Watch later JSON result could not be recorded", ex);
        }
    }

    public static void nativeResponse(Object request, Object response) {
        try {
            record(WatchLaterConfirmation.fromNative(request, response));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Logger.printInfo(() -> "Watch later completion could not be read", ex);
        }
    }

    private static void record(Map<String, String> entries) {
        if (entries.isEmpty()) return;
        Instant confirmedAt = Instant.now();
        Utils.runOnBackgroundThread(() -> {
            try {
                var context = Utils.getContext();
                if (context == null) return;
                var db = WatchLaterDates.open(context);
                for (var row : entries.entrySet()) db.confirm(row.getKey(), row.getValue(), confirmedAt);
            } catch (RuntimeException ex) {
                Logger.printInfo(() -> "Watch later confirmation could not be stored", ex);
            }
        });
    }
}
