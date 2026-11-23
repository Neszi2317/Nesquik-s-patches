/*
 * Playlist renderer parsing adapted from Morphe PlaylistSearchRequest, copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/5200
 * Modified by Nesquik to filter by publication date and report coverage.
 * See the included NOTICE for GPLv3 Section 7 terms.
 */
package app.morphe.extension.youtube.patches.utils.requests;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.youtube.patches.utils.requests.ChannelSearchRequest.ChannelSearchResult;
import fr.nesquik.playlistdate.DateRange;
import fr.nesquik.playlistdate.PublicationDates;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

public final class DatedPlaylistSearch {
    public enum State { LOADING, COMPLETE, PARTIAL, FAILED }

    public static final class Results {
        public final String title;
        public final List<ChannelSearchResult> matches;
        public final int examined;
        public final int unknown;
        public final int unavailable;
        public final boolean rateLimited;
        public final State state;

        Results(String title, List<ChannelSearchResult> matches, int examined, int unknown,
                int unavailable, boolean rateLimited, State state) {
            this.title = title;
            this.matches = matches;
            this.examined = examined;
            this.unknown = unknown;
            this.unavailable = unavailable;
            this.rateLimited = rateLimited;
            this.state = state;
        }
    }

    private static final class Video {
        final ChannelSearchResult result;
        final String text;

        Video(ChannelSearchResult result, String text) {
            this.result = result;
            this.text = text;
        }
    }

    private DatedPlaylistSearch() {}

    public static void fetch(String id, String query, DateRange range, Map<String, String> headers,
                             BooleanSupplier cancelled, Consumer<Results> onResults) {
        Utils.runOnBackgroundThread(() -> {
            Scan scan = new Scan(query, range, cancelled, onResults);
            try {
                var outcome = GetPlaylistItemsRequest.fetchPages(
                        PlaylistRoutes.BROWSE_PLAYLIST_SEARCH_VIDEOS, id, headers, scan::page);
                if (cancelled.getAsBoolean()) return;
                State state = switch (outcome) {
                    case FAILED -> State.FAILED;
                    case PARTIAL -> State.PARTIAL;
                    case COMPLETE -> scan.pages == 0 ? State.FAILED
                            : (scan.hasContinuation ? State.PARTIAL : State.COMPLETE);
                };
                scan.report(state);
            } catch (Exception ex) {
                Logger.printInfo(() -> "Playlist date search could not finish listing", ex);
                if (!cancelled.getAsBoolean()) scan.report(scan.pages == 0 ? State.FAILED : State.PARTIAL);
            }
        });
    }

    private static final class Scan {
        final DateRange range;
        final BooleanSupplier cancelled;
        final Consumer<Results> onResults;
        final List<String> words;
        final List<ChannelSearchResult> matches = new ArrayList<>();
        final Map<String, LocalDate> dates = new HashMap<>();
        String title = "";
        int pages;
        int examined;
        int unknown;
        int unavailable;
        boolean hasContinuation;
        boolean rateLimited;

        Scan(String query, DateRange range, BooleanSupplier cancelled, Consumer<Results> onResults) {
            this.range = range;
            this.cancelled = cancelled;
            this.onResults = onResults;
            this.words = ChannelUploadsRequest.normalizedWords(query);
        }

        boolean page(JSONObject response, JSONObject page) {
            if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) return false;
            if (pages++ == 0) {
                JSONObject header = response.optJSONObject("header");
                JSONObject renderer = header == null ? null : header.optJSONObject("pageHeaderRenderer");
                if (renderer != null) title = renderer.optString("pageTitle");
            }
            JSONArray continuation = page.optJSONArray("continuations");
            hasContinuation = continuation != null && continuation.length() > 0;
            JSONArray contents = page.optJSONArray("contents");
            if (contents != null) {
                List<Video> candidates = new ArrayList<>();
                for (int i = 0; i < contents.length(); i++) {
                    JSONObject entry = contents.optJSONObject(i);
                    JSONObject renderer = entry == null ? null : entry.optJSONObject("playlistVideoRenderer");
                    if (renderer == null) continue;
                    examined++;
                    Video video = parseVideo(renderer);
                    if (video == null) {
                        unavailable++;
                    } else if (words.isEmpty() || ChannelUploadsRequest.matches(video.text, words)) {
                        candidates.add(video);
                    }
                }
                if (!checkDates(candidates)) return false;
            }
            report(State.LOADING);
            return true;
        }

        boolean checkDates(List<Video> candidates) {
            Map<String, Future<LocalDate>> requests = new HashMap<>();
            try {
                for (Video candidate : candidates) {
                    String id = candidate.result.videoId;
                    if (!dates.containsKey(id)) {
                        requests.computeIfAbsent(id, key -> PublicationDates.workers()
                                .submit(() -> PublicationDates.resolve(key, cancelled)));
                    }
                }
                // Iterate candidates, not futures, to preserve playlist order and duplicate entries.
                for (Video candidate : candidates) {
                    if (cancelled.getAsBoolean()) return false;
                    String id = candidate.result.videoId;
                    if (!dates.containsKey(id)) dates.put(id, requests.get(id).get());
                    switch (range.match(dates.get(id))) {
                        case IN_RANGE -> matches.add(candidate.result);
                        case UNKNOWN -> unknown++;
                        case OUT_OF_RANGE -> { }
                    }
                }
                return true;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            } catch (java.util.concurrent.ExecutionException ex) {
                Logger.printInfo(() -> "Publication date lookup failed", ex);
                return false;
            } finally {
                for (Future<LocalDate> request : requests.values()) {
                    if (!request.isDone()) request.cancel(true);
                }
            }
        }

        void report(State state) {
            rateLimited |= PublicationDates.isRateLimited();
            onResults.accept(new Results(title, new ArrayList<>(matches), examined, unknown, unavailable,
                    rateLimited, state));
        }
    }

    private static Video parseVideo(JSONObject renderer) {
        String id = renderer.optString("videoId");
        String title = ChannelSearchRequest.parseText(renderer.optJSONObject("title"));
        if (id.isEmpty() || title.isEmpty()) return null;

        String channelName = ChannelSearchRequest.parseText(renderer.optJSONObject("shortBylineText"));
        List<String> details = new ArrayList<>();
        if (!channelName.isEmpty()) details.add(channelName);
        JSONObject info = renderer.optJSONObject("videoInfo");
        JSONArray runs = info == null ? null : info.optJSONArray("runs");
        for (int i = 0; runs != null && i < runs.length(); i++) {
            JSONObject run = runs.optJSONObject(i);
            String text = run == null ? "" : run.optString("text").trim();
            if (!text.isEmpty() && !"•".equals(text)) details.add(text);
        }
        JSONObject thumb = renderer.optJSONObject("thumbnail");
        ChannelSearchResult result = ChannelSearchRequest.createResult(id, title,
                thumb == null ? "" : ChannelSearchRequest.widestThumbnail(thumb.optJSONArray("thumbnails")),
                ChannelSearchRequest.parseText(renderer.optJSONObject("lengthText")), details);
        return new Video(result, title + "\n" + channelName);
    }
}
