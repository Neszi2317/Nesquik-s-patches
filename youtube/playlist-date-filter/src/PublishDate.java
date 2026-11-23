package fr.nesquik.playlistdate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import org.json.JSONObject;

public final class PublishDate {
    private PublishDate() {}

    public static LocalDate read(JSONObject response) {
        JSONObject microformat = response.optJSONObject("microformat");
        JSONObject player = microformat == null ? null : microformat.optJSONObject("playerMicroformatRenderer");
        return player == null ? null : parse(player.optString("publishDate", ""));
    }

    private static LocalDate parse(String value) {
        try {
            if (value.length() == 10) return LocalDate.parse(value);
            // The source's calendar day is authoritative, including around midnight.
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
