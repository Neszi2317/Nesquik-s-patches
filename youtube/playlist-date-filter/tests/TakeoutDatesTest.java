package fr.nesquik.playlistdate;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;

class TakeoutDatesTest {
    private static final String HEADER = "Video ID,Playlist Video Creation Timestamp\n";
    private static int checks;

    public static void main(String[] args) throws Exception {
        var parsed = TakeoutDates.read(new StringReader(HEADER
                + "jNQXAC9IVRw,2019-12-17 10:36:20 UTC\n"
                + "dQw4w9WgXcQ,2026-11-23T19:22:45+01:00\n"
                + "jNQXAC9IVRw,2019-12-17 10:36:20 UTC\n"));
        expect(parsed.size() == 2);
        expect(Instant.parse("2019-12-17T10:36:20Z").equals(parsed.get("jNQXAC9IVRw")));
        expect(Instant.parse("2026-11-23T18:22:45Z").equals(parsed.get("dQw4w9WgXcQ")));
        expect(TakeoutDates.read(new StringReader("\uFEFF\"Video ID\";\"Playlist Video Creation Timestamp\"\r\n"
                + "\"jNQXAC9IVRw\";\"2005-04-23T20:31:52-07:00\"\r\n")).size() == 1);
        expect(TakeoutDates.read(new StringReader(HEADER)).isEmpty());
        var french = TakeoutDates.read(new StringReader(
                "ID vidéo,Code temporel de création de la vidéo de la playlist\n"
                + " jNQXAC9IVRw,2026-11-23T19:22:45+00:00\n\n"));
        expect(french.size() == 1);
        expect(Instant.parse("2026-11-23T19:22:45Z").equals(french.get("jNQXAC9IVRw")));
        reject(HEADER + "jNQXAC9IVRw,2024-02-30 10:36:20 UTC\n");
        reject(HEADER + "jNQXAC9IVRw,2020-02-12T10:36:20\n");
        reject(HEADER + "wrongid,2024-02-19T10:36:20Z\n");
        reject(HEADER + "jNQXAC9IVRw,2024-02-19T10:36:20Z\njNQXAC9IVRw,2025-02-19T10:36:20Z\n");
        reject("Video ID,Video Publication Date\njNQXAC9IVRw,2024-02-19T10:36:20Z\n");
        reject(HEADER + "\"jNQXAC9IVRw,2024-02-19T10:36:20Z\n");
        System.out.println(checks + " successful Takeout assertions");
    }

    private static void reject(String csv) throws Exception {
        try {
            TakeoutDates.read(new StringReader(csv));
            throw new AssertionError("Unexpectedly accepted CSV");
        } catch (IOException expected) {
            checks++;
        }
    }

    private static void expect(boolean condition) {
        if (!condition) throw new AssertionError("Unexpected Takeout result");
        checks++;
    }
}
