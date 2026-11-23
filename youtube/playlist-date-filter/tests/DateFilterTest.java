import fr.nesquik.playlistdate.DateRange;
import fr.nesquik.playlistdate.DateRange.Match;
import fr.nesquik.playlistdate.PublishDate;
import java.time.LocalDate;
import org.json.JSONObject;

class DateFilterTest {
    private static int checks;

    public static void main(String[] args) {
        LocalDate from = LocalDate.parse("2024-11-02");
        LocalDate to = LocalDate.parse("2024-12-04");
        DateRange period = new DateRange(from, to);
        expect(period.isValid(), "normal period");
        expect(period.match(from) == Match.IN_RANGE, "inclusive lower bound");
        expect(period.match(to) == Match.IN_RANGE, "inclusive upper bound");
        expect(period.match(from.plusDays(1)) == Match.IN_RANGE, "inside");
        expect(period.match(from.minusDays(1)) == Match.OUT_OF_RANGE, "before");
        expect(period.match(to.plusDays(1)) == Match.OUT_OF_RANGE, "after");
        expect(period.match(null) == Match.UNKNOWN, "absent date remains unclassified");

        DateRange single = new DateRange(from, from);
        expect(single.isValid() && single.match(from) == Match.IN_RANGE, "same date");
        expect(single.match(from.plusDays(1)) == Match.OUT_OF_RANGE, "single day");

        DateRange fromOnly = new DateRange(from, null);
        expect(fromOnly.match(to.plusYears(10)) == Match.IN_RANGE, "no upper bound");
        expect(fromOnly.match(from.minusDays(1)) == Match.OUT_OF_RANGE, "start only rejects earlier");
        DateRange untilOnly = new DateRange(null, to);
        expect(untilOnly.match(from.minusYears(10)) == Match.IN_RANGE, "no lower bound");
        expect(untilOnly.match(to.plusDays(1)) == Match.OUT_OF_RANGE, "end only rejects later");
        expect(!new DateRange(to, from).isValid(), "reject reversed range");
        expect(DateRange.EMPTY.isEmpty() && DateRange.EMPTY.match(null) == Match.IN_RANGE, "clear dates");

        expect(parse(from.toString()).equals(from), "exact date");
        expect(parse("2024-11-02T23:30:00-07:00").equals(from), "calendar day in source time zone");
        expect(parse("2024-11-02T00:01:00+09:00").equals(from), "offset retained across midnight");
        expect(parse("2024-11-02T08:00:00Z").equals(from), "UTC timestamp");
        expect(parse("2024-11-02T23:30:00") == null, "timestamp with no source offset");
        expect(parse("2024-02-30") == null, "invalid day");
        expect(parse("2 weeks ago") == null, "no approximation from relative text");
        expect(PublishDate.read(new JSONObject()) == null, "no microformat");
        expect(PublishDate.read(new JSONObject("""
                {"microformat":{"playerMicroformatRenderer":{"uploadDate":"2024-11-02"}}}
                """)) == null, "upload date is separate");
        expect(PublishDate.read(new JSONObject("""
                {"snippet":{"publishedAt":"2024-11-02","videoPublishedAt":"2024-11-02"}}
                """)) == null, "never use playlist item timestamps");
        expect(PublishDate.read(new JSONObject("""
                {"playabilityStatus":{"status":"LOGIN_REQUIRED"}}
                """)) == null, "unavailable video remains unknown");
        System.out.println(checks + " successful assertions");
    }

    private static LocalDate parse(String value) {
        JSONObject renderer = new JSONObject().put("publishDate", value);
        return PublishDate.read(new JSONObject().put("microformat",
                new JSONObject().put("playerMicroformatRenderer", renderer)));
    }

    private static void expect(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        checks++;
    }
}
