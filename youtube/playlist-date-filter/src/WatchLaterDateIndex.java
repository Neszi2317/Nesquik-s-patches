package fr.nesquik.playlistdate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Associates an imported video with a live playlist entry when it is first seen. */
public final class WatchLaterDateIndex {
    record Imported(long added, String entry) {}

    private final Map<String, Long> confirmed;
    private final Map<String, Imported> imported;
    private final ZoneId zone;
    private final Consumer<Map<String, Imported>> saveBindings;
    private final Map<String, Imported> bindings = new HashMap<>();

    WatchLaterDateIndex(Map<String, Long> confirmed, Map<String, Imported> imported, ZoneId zone,
                        Consumer<Map<String, Imported>> saveBindings) {
        this.confirmed = confirmed;
        this.imported = new HashMap<>(imported);
        this.zone = zone;
        this.saveBindings = saveBindings;
    }

    static String key(String video, String entry) {
        return entry + ":" + video;
    }

    public LocalDate resolve(String video, String entry) {
        if (entry.isEmpty()) return null;
        Long exact = confirmed.get(key(video, entry));
        Imported historical = imported.get(video);
        if (historical != null && historical.entry() == null) {
            historical = new Imported(historical.added(), entry);
            imported.put(video, historical);
            bindings.put(video, historical);
        }
        if (exact != null) return day(exact);
        return historical != null && entry.equals(historical.entry()) ? day(historical.added()) : null;
    }

    public void savePage() {
        if (bindings.isEmpty()) return;
        saveBindings.accept(bindings);
        bindings.clear();
    }

    private LocalDate day(long time) {
        return Instant.ofEpochMilli(time).atZone(zone).toLocalDate();
    }
}
