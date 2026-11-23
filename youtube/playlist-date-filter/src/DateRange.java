package fr.nesquik.playlistdate;

import java.time.LocalDate;

public final class DateRange {
    private final LocalDate start;
    private final LocalDate end;

    public DateRange(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
    }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }

    public static final DateRange EMPTY = new DateRange(null, null);

    public boolean isEmpty() {
        return start == null && end == null;
    }

    public boolean isValid() {
        return start == null || end == null || !start.isAfter(end);
    }

    public enum Match { IN_RANGE, OUT_OF_RANGE, UNKNOWN }

    public Match match(LocalDate date) {
        if (!isValid()) throw new IllegalStateException("Reversed date range");
        if (isEmpty()) return Match.IN_RANGE;
        if (date == null) return Match.UNKNOWN;
        if ((start != null && date.isBefore(start)) || (end != null && date.isAfter(end))) {
            return Match.OUT_OF_RANGE;
        }
        return Match.IN_RANGE;
    }
}
