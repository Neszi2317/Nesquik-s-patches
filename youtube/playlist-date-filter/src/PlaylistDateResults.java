package fr.nesquik.playlistdate;

import static app.morphe.extension.shared.StringRef.str;
import static fr.nesquik.playlistdate.PlaylistDateDialog.dp;
import static fr.nesquik.playlistdate.PlaylistDateDialog.phrase;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.innertube.utils.AuthUtils;
import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.youtube.patches.utils.SearchResultsSheet;
import app.morphe.extension.youtube.patches.utils.requests.DatedPlaylistSearch;
import app.morphe.extension.youtube.patches.utils.requests.DatedPlaylistSearch.Results;
import app.morphe.extension.youtube.patches.utils.requests.DatedPlaylistSearch.State;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

final class PlaylistDateResults {
    // The standard Morphe sheet uses a LinearLayout. Page the visible views so a wide
    // date interval cannot create thousands of thumbnails and views on the UI thread.
    private static final int PAGE_SIZE = 60;

    private final String playlistId;
    private final Locale locale;
    private final SearchResultsSheet sheet;
    private final TextView status;
    private final LinearLayout navigation;
    private final Button previous;
    private final Button next;
    private volatile boolean cancelled;
    private Results results;
    private int page;

    static void show(Activity activity, Locale locale, String playlistId, String query, DateRange range,
                     boolean byAddition) {
        var view = new PlaylistDateResults(activity, locale, playlistId, query, range, byAddition);
        DatedPlaylistSearch.fetch(playlistId, query, range, AuthUtils.getRequestHeader(),
                () -> view.cancelled, view::receive,
                byAddition ? () -> WatchLaterDates.open(activity).snapshot() : null);
    }

    private PlaylistDateResults(Activity activity, Locale locale, String playlistId, String query, DateRange range,
                                boolean byAddition) {
        this.playlistId = playlistId;
        this.locale = locale;
        var bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 8));
        status = PlaylistDateDialog.label(activity, text("Lecture de la playlist…", "Reading playlist…"));
        bar.addView(status);
        previous = CustomDialog.createButton(activity, null, text("Précédent", "Previous"),
                () -> changePage(-1), false, false);
        next = CustomDialog.createButton(activity, null, text("Suivant", "Next"),
                () -> changePage(1), false, false);
        navigation = CustomDialog.createButtonRow(activity, previous, next);
        navigation.setVisibility(View.GONE);
        bar.addView(navigation);
        String period = periodLabel(range);
        if (byAddition) period = text("Date d’ajout", "Date added") + " · " + period;
        sheet = new SearchResultsSheet(activity, query.isEmpty() ? period : query + " · " + period,
                str("morphe_playlist_search_results"), bar, () -> cancelled = true);
    }

    private void receive(Results updated) {
        Utils.runOnMainThreadNowOrLater(() -> {
            if (cancelled || !sheet.isShowing()) return;
            results = updated;
            render();
        });
    }

    private void changePage(int direction) {
        int updated = page + direction;
        if (results == null || updated < 0 || updated * PAGE_SIZE >= results.matches.size()) return;
        page = updated;
        render();
        sheet.scrollToTop();
    }

    private void render() {
        sheet.setScopeName(results.title);
        int count = results.matches.size();
        int start = page * PAGE_SIZE;
        int end = Math.min(count, start + PAGE_SIZE);
        boolean loading = results.state == State.LOADING;
        if (count > 0) {
            sheet.showVideos(results.matches.subList(start, end), playlistId, loading && end == count);
        } else if (!loading) {
            sheet.showState(results.state == State.FAILED ? str("morphe_playlist_search_failed")
                    : text("Aucune vidéo confirmée pour cette période.", "No videos confirmed for this period."));
        }
        navigation.setVisibility(count > PAGE_SIZE ? View.VISIBLE : View.GONE);
        previous.setEnabled(page > 0);
        next.setEnabled(end < count);

        var summary = new StringBuilder();
        if (count > PAGE_SIZE) {
            summary.append(String.format(locale, "%d–%d / %d", start + 1, end, count)).append(" · ");
        }
        summary.append(String.format(locale, text("%d entrées lues · %d dates inconnues",
                "%d entries read · %d dates unknown"), results.examined, results.unknown));
        if (results.unavailable > 0) {
            summary.append(" · ").append(String.format(locale, text("%d entrées illisibles",
                    "%d unreadable entries"), results.unavailable));
        }
        if (loading) summary.append("\n").append(text("Recherche en cours…", "Searching…"));
        if (results.state == State.PARTIAL) {
            summary.append("\n").append(text("Certaines pages de la playlist manquent.",
                    "Some playlist pages could not be read."));
        }
        if (results.rateLimited && results.unknown > 0) {
            summary.append("\n").append(text("YouTube limite les requêtes. Réessaie plus tard.",
                    "YouTube is limiting requests. Try again later."));
        }
        status.setText(summary);
    }

    private String periodLabel(DateRange range) {
        var format = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale);
        LocalDate start = range.start();
        LocalDate end = range.end();
        if (start == null) return text("Jusqu’au ", "Through ") + format.format(end);
        if (end == null) return text("Depuis le ", "From ") + format.format(start);
        if (start.equals(end)) return format.format(start);
        return format.format(start) + " – " + format.format(end);
    }

    private String text(String french, String english) {
        return phrase(locale, french, english);
    }
}
