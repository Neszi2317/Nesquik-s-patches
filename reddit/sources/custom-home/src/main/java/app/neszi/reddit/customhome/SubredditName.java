package app.neszi.reddit.customhome;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/** Pure Java: no Android dependency; rejects paths, fake Reddit hosts and feed aliases. */
public final class SubredditName {
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{2,21}");
    public static String normalize(String input) {
        if (input == null) throw new IllegalArgumentException("Enter a subreddit name");
        String value = input.trim();
        if (value.regionMatches(true, 0, "https://", 0, 8) || value.regionMatches(true, 0, "http://", 0, 7)) {
            URI uri;
            try { uri = URI.create(value); } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid Reddit URL"); }
            String host = uri.getHost();
            if (host == null || !(host.equalsIgnoreCase("reddit.com") || host.equalsIgnoreCase("www.reddit.com") || host.equalsIgnoreCase("old.reddit.com"))
                    || uri.getRawUserInfo() != null || uri.getPort() != -1 || uri.getRawQuery() != null || uri.getRawFragment() != null)
                throw new IllegalArgumentException("Use a direct reddit.com/r/name/ URL");
            value = uri.getRawPath();
            if (value == null || !value.regionMatches(true, 0, "/r/", 0, 3)) throw new IllegalArgumentException("Use a subreddit URL");
            value = value.substring(3);
            if (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        } else {
            if (value.regionMatches(true, 0, "r/", 0, 2)) value = value.substring(2);
        }
        if (!NAME.matcher(value).matches()) throw new IllegalArgumentException("Use 2–21 letters, numbers or underscores");
        value = value.toLowerCase(Locale.ROOT);
        if (value.equals("all") || value.equals("popular") || value.equals("home") || value.equals("friends") || value.equals("mod"))
            throw new IllegalArgumentException("This is a global feed, not a subreddit");
        return value;
    }
    private SubredditName() {}
}
