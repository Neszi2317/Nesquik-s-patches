package fr.nesquik.playlistdate;

import android.os.SystemClock;
import app.morphe.extension.shared.requests.Requester;
import app.morphe.extension.shared.requests.Route;
import app.morphe.extension.youtube.patches.utils.requests.ChannelSearchRoutes;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.json.JSONException;

public final class PublicationDates {
    private static final int MAX_CACHED_DATES = 6000;
    private static final long CACHE_MS = TimeUnit.HOURS.toMillis(24);
    private static final long BETWEEN_REQUESTS_MS = 150;
    private static final long RATE_LIMIT_PAUSE_MS = TimeUnit.MINUTES.toMillis(2);
    private static final Route.CompiledRoute DATE_ONLY = new Route(Route.Method.POST,
            "player?prettyPrint=false&fields=microformat.playerMicroformatRenderer.publishDate").compile();

    private static final class CachedDate {
        final LocalDate date;
        final long fetchedAt;

        CachedDate(LocalDate date, long fetchedAt) {
            this.date = date;
            this.fetchedAt = fetchedAt;
        }
    }

    private static final Map<String, CachedDate> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CachedDate> eldest) {
                    return size() > MAX_CACHED_DATES;
                }
            });
    private static final ThreadPoolExecutor workers = new ThreadPoolExecutor(
            4, 4, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private static long nextRequestMs;
    private static volatile long pausedUntilMs;

    static {
        workers.allowCoreThreadTimeOut(true);
    }

    private PublicationDates() {}

    public static ExecutorService workers() {
        return workers;
    }

    public static boolean isRateLimited() {
        return nowMs() < pausedUntilMs;
    }

    public static LocalDate resolve(String videoId, BooleanSupplier cancelled) {
        CachedDate cached = cache.get(videoId);
        if (cached != null && nowMs() - cached.fetchedAt < CACHE_MS) return cached.date;
        if (cancelled.getAsBoolean() || isRateLimited()) return null;

        HttpURLConnection connection = null;
        try {
            if (!awaitRequestSlot(cancelled)) return null;
            byte[] body = ChannelSearchRoutes.createVideoBody(videoId, Locale.US);
            connection = ChannelSearchRoutes.getConnection(DATE_ONLY);
            connection.setFixedLengthStreamingMode(body.length);
            try (var stream = connection.getOutputStream()) {
                stream.write(body);
            }
            int status = connection.getResponseCode();
            if (status == 429 || status == 503) {
                pausedUntilMs = nowMs() + RATE_LIMIT_PAUSE_MS;
            }
            if (status != 200) return null;
            var response = Requester.parseJSONObject(connection);
            // Requester closed the stream. Leave the successful connection reusable.
            connection = null;
            LocalDate date = PublishDate.read(response);
            if (date != null) cache.put(videoId, new CachedDate(date, nowMs()));
            return date;
        } catch (IOException | JSONException | RuntimeException ignored) {
            return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static boolean awaitRequestSlot(BooleanSupplier cancelled) throws InterruptedException {
        final long slot;
        synchronized (PublicationDates.class) {
            long now = nowMs();
            slot = Math.max(now, nextRequestMs);
            nextRequestMs = slot + BETWEEN_REQUESTS_MS;
        }
        long delay = slot - nowMs();
        if (delay > 0) Thread.sleep(delay);
        return !cancelled.getAsBoolean() && !isRateLimited();
    }

    private static long nowMs() {
        return SystemClock.elapsedRealtime();
    }
}
