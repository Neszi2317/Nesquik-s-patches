package fr.nesquik.playlistdate;

import fr.nesquik.playlistdate.WatchLaterDateIndex.Imported;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class WatchLaterDateIndexTest {
    private static int checks;

    public static void main(String[] args) {
        String video = "abcdefghijk";
        long old = Instant.parse("2024-11-02T23:30:00Z").toEpochMilli();
        long recent = Instant.parse("2026-11-23T16:04:00Z").toEpochMilli();
        LocalDate historical = LocalDate.of(2024, 11, 3);
        LocalDate confirmedDay = LocalDate.of(2026, 11, 23);
        ZoneId zone = ZoneId.of("Europe/Paris");
        Map<String, Imported> disk = new HashMap<>(Map.of(video, new Imported(old, null)));
        var index = new WatchLaterDateIndex(Map.of(), disk, zone, disk::putAll);
        expect(null, index.resolve(video, ""));
        expect(null, index.resolve("elsewhere01", "entry-x"));
        index.savePage();
        expect(null, disk.get(video).entry());
        expect(historical, index.resolve(video, "entry-a"));
        expect(null, index.resolve(video, "entry-b"));
        expect(null, disk.get(video).entry());
        index.savePage();
        expect("entry-a", disk.get(video).entry());
        expect(old, disk.get(video).added());

        Map<String, Long> confirmed = Map.of(WatchLaterDateIndex.key(video, "entry-b"), recent);
        var reopened = new WatchLaterDateIndex(confirmed, disk, zone, disk::putAll);
        expect(historical, reopened.resolve(video, "entry-a"));
        expect(confirmedDay, reopened.resolve(video, "entry-b"));
        expect(null, reopened.resolve(video, "entry-c"));
        var anotherScope = new WatchLaterDateIndex(confirmed, Map.of(), zone, ignored -> {});
        expect(null, anotherScope.resolve(video, "entry-a"));

        var reimport = new WatchLaterDateIndex(confirmed, Map.of(video, new Imported(old, null)),
                zone, disk::putAll);
        expect(confirmedDay, reimport.resolve(video, "entry-b"));
        reimport.savePage();
        expect("entry-b", disk.get(video).entry());
        expect(null, reimport.resolve(video, "entry-c"));
        System.out.println("WatchLaterDateIndexTest passed " + checks + " assertions");
    }

    private static void expect(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
        checks++;
    }
}
