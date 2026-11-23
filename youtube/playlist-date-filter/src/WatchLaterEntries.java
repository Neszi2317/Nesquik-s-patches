package app.morphe.extension.youtube.patches.utils.requests;

import fr.nesquik.playlistdate.WatchLaterDates;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Match imported video IDs against the account's current playlist-entry IDs. */
public final class WatchLaterEntries {
    private WatchLaterEntries() {}

    public static Map<String, String> fetch(Map<String, String> headers) throws Exception {
        Map<String, String> entries = new HashMap<>();
        boolean[] reachedLastPage = {false};
        var outcome = GetPlaylistItemsRequest.fetchPages(PlaylistRoutes.BROWSE_PLAYLIST_VIDEO_IDS,
                WatchLaterDates.PLAYLIST, headers, (response, page) -> {
                    JSONArray rows = page.optJSONArray("contents");
                    if (rows != null) {
                        for (int i = 0; i < rows.length(); i++) {
                            JSONObject row = rows.optJSONObject(i);
                            JSONObject video = row == null ? null : row.optJSONObject("playlistVideoRenderer");
                            if (video == null) continue;
                            String videoId = video.optString("videoId");
                            String entry = video.optString("setVideoId");
                            if (!videoId.isEmpty() && !entry.isEmpty()) entries.put(videoId, entry);
                        }
                    }
                    JSONArray continuation = page.optJSONArray("continuations");
                    reachedLastPage[0] = continuation == null || continuation.length() == 0;
                    return true;
                });
        if (outcome != GetPlaylistItemsRequest.PagesResult.COMPLETE || !reachedLastPage[0]) {
            throw new IOException("Watch later could not be listed completely");
        }
        return entries;
    }
}
