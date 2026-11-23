package fr.nesquik.playlistdate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class TakeoutDates {
    private static final int MAX_ROWS = 20_000;
    private static final int MAX_LINE = 4096;
    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final DateTimeFormatter TAKEOUT_UTC =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss 'UTC'", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);

    private TakeoutDates() {}

    static Map<String, Instant> read(Reader reader) throws IOException {
        var input = new BufferedReader(reader);
        String header = input.readLine();
        if (header == null || header.length() > MAX_LINE) throw new IOException("Missing CSV header");
        if (header.startsWith("\uFEFF")) header = header.substring(1);
        char delimiter = header.indexOf(',') >= 0 ? ',' : header.indexOf(';') >= 0 ? ';' : '\t';
        List<String> fields = split(header, delimiter);
        int id = find(fields, "video id", "videoid", "id vidéo", "id de la vidéo", "identifiant de la vidéo");
        int time = find(fields, "playlist video creation timestamp", "time added", "date added",
                "code temporel de création de la vidéo de la playlist",
                "horodatage de création de la vidéo dans la playlist", "date d'ajout");
        if (id < 0 || time < 0) throw new IOException("Expected Takeout video IDs and addition timestamps");

        Map<String, Instant> dates = new HashMap<>();
        String line;
        int row = 0;
        while ((line = input.readLine()) != null) {
            if (++row > MAX_ROWS || line.length() > MAX_LINE) throw new IOException("CSV is too large");
            if (line.trim().isEmpty()) continue;
            List<String> cells = split(line, delimiter);
            if (cells.size() != fields.size() || !VIDEO_ID.matcher(cells.get(id)).matches()) {
                throw new IOException("Invalid CSV row " + row);
            }
            var instant = parse(cells.get(time));
            Instant previous = dates.putIfAbsent(cells.get(id), instant);
            if (previous != null && !previous.equals(instant)) {
                throw new IOException("Conflicting dates for a video in the CSV");
            }
        }
        return dates;
    }

    private static Instant parse(String timestamp) throws IOException {
        try {
            return OffsetDateTime.parse(timestamp).toInstant();
        } catch (DateTimeParseException unused) {
            try {
                return LocalDateTime.parse(timestamp, TAKEOUT_UTC).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException invalid) {
                throw new IOException("Unrecognized addition timestamp", invalid);
            }
        }
    }

    private static int find(List<String> fields, String... names) {
        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i).toLowerCase(Locale.ROOT).replace('’', '\'');
            for (String name : names) {
                if (name.equals(field)) return i;
            }
        }
        return -1;
    }

    private static List<String> split(String line, char delimiter) throws IOException {
        List<String> cells = new ArrayList<>();
        int index = 0;
        for (;;) {
            String value;
            if (index < line.length() && line.charAt(index) == '"') {
                StringBuilder quoted = new StringBuilder();
                boolean closed = false;
                for (index++; index < line.length(); index++) {
                    char current = line.charAt(index);
                    if (current == '"' && (index + 1 == line.length() || line.charAt(index + 1) != '"')) {
                        index++;
                        closed = true;
                        break;
                    }
                    if (current == '"') index++;
                    quoted.append(current);
                }
                if (!closed || (index < line.length() && line.charAt(index) != delimiter)) {
                    throw new IOException("Invalid quoted CSV field");
                }
                value = quoted.toString();
            } else {
                int start = index;
                while (index < line.length() && line.charAt(index) != delimiter) index++;
                value = line.substring(start, index);
                if (value.indexOf('"') >= 0) throw new IOException("Invalid CSV quote");
            }
            cells.add(value.trim());
            if (index == line.length()) return cells;
            index++;
        }
    }
}
