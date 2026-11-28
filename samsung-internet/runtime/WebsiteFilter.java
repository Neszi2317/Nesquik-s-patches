// SPDX-License-Identifier: MPL-2.0
package fr.nesquik.sbrowser.blocker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Called with the canonical URL from Chromium's network loader. */
public final class WebsiteFilter {
    private final Set<String> domains = new HashSet<>();
    private final Map<String, Set<String>> paths = new HashMap<>();
    private static volatile boolean stockSupported = true;

    private static final class Holder {
        static final WebsiteFilter RULES = new WebsiteFilter();
        static { populate(null, RULES); }
    }

    // The patch adds const-string instructions here before packaging the APK.
    private static void populate(String literal, WebsiteFilter rules) {}

    public static boolean rememberSupport(boolean original) {
        stockSupported = original;
        return true;
    }

    public static boolean stockEnabled() { return stockSupported; }

    public static boolean blocked(String url) { return Holder.RULES.matches(url); }

    void addChunk(String chunk) {
        for (String rule : chunk.split("\n")) {
            int slash = rule.indexOf('/');
            if (slash < 0) domains.add(rule);
            else paths.computeIfAbsent(rule.substring(0, slash), k -> new HashSet<>())
                    .add(rule.substring(slash));
        }
    }

    // Chromium has already canonicalized the host, escaped user info, and serialized the URL.
    boolean matches(String url) {
        if (url == null) return false;
        int start;
        if (url.regionMatches(true, 0, "https://", 0, 8)) start = 8;
        else if (url.regionMatches(true, 0, "http://", 0, 7)) start = 7;
        else return false;

        int end = start, hostStart = start;
        while (end < url.length()) {
            char c = url.charAt(end);
            if (c == '/' || c == '?' || c == '#') break;
            if (c == '@') hostStart = end + 1;
            end++;
        }
        if (end == hostStart) return false;
        String hostPort = url.substring(hostStart, end);
        boolean ipv6 = hostPort.startsWith("[");
        int cut = ipv6 ? hostPort.indexOf(']') : hostPort.indexOf(':');
        if (ipv6 && cut < 0) return false;
        String host = (cut < 0 ? hostPort : hostPort.substring(0, ipv6 ? cut + 1 : cut))
                .toLowerCase(Locale.ROOT);
        if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        boolean address = ipv6 || numericHost(host);
        String path = null;
        while (!host.isEmpty()) {
            if (domains.contains(host)) return true;
            Set<String> prefixes = paths.get(host);
            if (prefixes != null) {
                if (path == null) path = url.substring(end).toLowerCase(Locale.ROOT);
                if (matchesPath(path, prefixes)) return true;
            }
            int dot = host.indexOf('.');
            if (address || dot < 0) break;
            host = host.substring(dot + 1);
        }
        return false;
    }

    private static boolean numericHost(String host) {
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if (c != '.' && (c < '0' || c > '9')) return false;
        }
        return true;
    }

    private static boolean matchesPath(String path, Set<String> prefixes) {
        // For small sets (or an unusually long URL), avoid allocating candidate substrings.
        if (prefixes.size() <= 8 || path.length() > 4096) {
            for (String prefix : prefixes) {
                if (!path.startsWith(prefix)) continue;
                int n = prefix.length();
                if (n == path.length()) return true;
                char next = path.charAt(n);
                if (next == '/' || next == '?' || next == '#' ||
                        (next == '&' && prefix.indexOf('?') >= 0)) return true;
            }
            return false;
        }
        if (prefixes.contains(path)) return true;
        boolean query = false;
        for (int i = 1; i < path.length(); i++) {
            char c = path.charAt(i);
            if ((c == '/' || c == '?' || c == '#' || (query && c == '&')) &&
                    prefixes.contains(path.substring(0, i))) return true;
            if (c == '?') query = true;
        }
        return false;
    }
}
