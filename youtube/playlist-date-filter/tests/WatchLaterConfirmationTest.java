package fr.nesquik.playlistdate;

import java.util.List;
import java.util.Map;
import org.json.JSONObject;

class WatchLaterConfirmationTest {
    private static final String VIDEO = "jNQXAC9IVRw";
    private static final String OTHER = "dQw4w9WgXcQ";
    private static final String ENTRY = "watch-later-entry-one";
    private static int checks;

    public static void main(String[] args) throws Exception {
        var addition = new Action(1, VIDEO);
        var removal = new Action(2, VIDEO);
        var result = new Result(138418859, new Added(VIDEO, ENTRY));
        var success = new Response(1, List.of(result));
        expect(WatchLaterConfirmation.fromNative(new Request("WL", List.of(addition)), success),
                Map.of(VIDEO, ENTRY));
        expect(WatchLaterConfirmation.fromNative(new Request("PLcustom", List.of(addition)), success),
                Map.of());
        expect(WatchLaterConfirmation.fromNative(new Request("WL", List.of(removal)), success), Map.of());
        expect(WatchLaterConfirmation.fromNative(new Request("WL", List.of(addition)),
                new Response(0, List.of(result))), Map.of());
        expect(WatchLaterConfirmation.fromNative(new Request("WL", List.of(addition)),
                new Response(1, List.of())), Map.of());
        expect(WatchLaterConfirmation.fromNative(new Request("WL", List.of(addition)),
                new Response(1, List.of(new Result(138418859, new Added(OTHER, ENTRY))))), Map.of());
        expect(WatchLaterConfirmation.fromNative(new Request("WL", List.of(addition)),
                new Response(1, List.of(new Result(0, new Added(VIDEO, ENTRY))))), Map.of());
        expect(WatchLaterConfirmation.fromNative(new Request("WL", List.of(addition, new Action(1, OTHER))),
                new Response(1, List.of(result, new Result(138418859, new Added(OTHER, "entry-two"))))),
                Map.of(VIDEO, ENTRY, OTHER, "entry-two"));

        checkJson(new JSONObject("""
                {"status":"STATUS_SUCCEEDED","playlistEditResults":[{"playlistEditVideoAddedResultData":
                  {"videoId":"jNQXAC9IVRw","setVideoId":"watch-later-entry-one"}}]}
                """), ENTRY);
        checkJson(new JSONObject("""
                {"status":"STATUS_SUCCEEDED","playlistEditResults":[{"playlistEditVideoAddedResultData":
                  {"videoId":"dQw4w9WgXcQ","setVideoId":"watch-later-entry-one"}}]}
                """), null);
        checkJson(new JSONObject("{\"status\":\"STATUS_SUCCEEDED\",\"playlistEditResults\":[]}"), null);
        checkJson(new JSONObject("{\"status\":\"STATUS_FAILED\"}"), null);
        checkJson(null, null);
        System.out.println(checks + " successful confirmation assertions");
    }

    private static void checkJson(JSONObject response, String expected) {
        expect(WatchLaterConfirmation.fromJson(VIDEO, response), expected);
    }

    private static void expect(Object actual, Object expected) {
        if (!java.util.Objects.equals(actual, expected)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
        checks++;
    }

    private record Request(String d, List<Action> e) {}
    private record Action(int d, String e) {}
    private record Response(int d, List<Result> j) {}
    private record Result(int b, Added c) {}
    private record Added(String c, String d) {}
}
