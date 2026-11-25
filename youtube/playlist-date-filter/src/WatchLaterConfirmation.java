package fr.nesquik.playlistdate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.json.JSONObject;

/** The response identifies the actual playlist entry created by YouTube. */
final class WatchLaterConfirmation {
    private static final int ADD_VIDEO_ACTION = 1;
    private static final int ADDED_VIDEO_RESULT = 138418859;
    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");

    private WatchLaterConfirmation() {}

    static String fromJson(String video, JSONObject response) {
        if (response == null || !VIDEO_ID.matcher(video).matches()
                || !"STATUS_SUCCEEDED".equals(response.optString("status"))) return null;
        var results = response.optJSONArray("playlistEditResults");
        if (results == null || results.length() != 1) return null;
        var result = results.optJSONObject(0);
        var added = result == null ? null : result.optJSONObject("playlistEditVideoAddedResultData");
        if (added == null || !video.equals(added.optString("videoId", video))) return null;
        String entry = added.optString("setVideoId");
        return entry.isEmpty() ? null : entry;
    }

    static Map<String, String> fromNative(Object request, Object response) throws ReflectiveOperationException {
        // Both supported builds use the same field names. The patch checks their per-build descriptors.
        if (!"WL".equals(field(request, "d")) || (int) field(response, "d") != 1) return Map.of();
        Set<String> requested = new HashSet<>();
        for (Object action : (List<?>) field(request, "e")) {
            if ((int) field(action, "d") != ADD_VIDEO_ACTION) continue;
            String video = (String) field(action, "e");
            if (VIDEO_ID.matcher(video).matches()) requested.add(video);
        }
        if (requested.isEmpty()) return Map.of();

        Map<String, String> confirmed = new HashMap<>();
        for (Object result : (List<?>) field(response, "j")) {
            if ((int) field(result, "b") != ADDED_VIDEO_RESULT) continue;
            Object added = field(result, "c");
            if (added == null) continue;
            String video = (String) field(added, "c");
            String entry = (String) field(added, "d");
            if (requested.contains(video) && !entry.isEmpty()) confirmed.put(video, entry);
        }
        return confirmed;
    }

    private static Object field(Object message, String name) throws ReflectiveOperationException {
        var field = message.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(message);
    }
}
