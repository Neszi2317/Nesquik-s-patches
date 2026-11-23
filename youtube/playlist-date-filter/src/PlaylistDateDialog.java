package fr.nesquik.playlistdate;

import static app.morphe.extension.shared.StringRef.str;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.InputType;
import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.theme.ThemeUtils;
import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.youtube.patches.utils.SearchDialog;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public final class PlaylistDateDialog {
    private static final int MAX_REMEMBERED_PLAYLISTS = 10;
    // Only accessed on the UI thread, and the values do not retain the activity.
    private static final Map<Activity, Map<String, DateRange>> remembered = new WeakHashMap<>();

    private PlaylistDateDialog() {}

    /** Replaces the one SearchDialog.show call in Morphe's Playlist search patch. */
    public static void show(Activity activity, String title, Consumer<String> originalSearch, String playlistId) {
        try {
            showWithDates(activity, title, originalSearch, playlistId);
        } catch (RuntimeException ex) {
            Logger.printException(() -> "Unable to add the playlist date controls", ex);
            SearchDialog.show(activity, title, originalSearch);
        }
    }

    private static void showWithDates(Activity activity, String title,
                                      Consumer<String> originalSearch, String playlistId) {
        Locale locale = activity.getResources().getConfiguration().getLocales().get(0);
        Map<String, DateRange> filters = remembered.computeIfAbsent(activity, ignored ->
                new LinkedHashMap<>(MAX_REMEMBERED_PLAYLISTS, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, DateRange> eldest) {
                        return size() > MAX_REMEMBERED_PLAYLISTS;
                    }
                });
        EditText edit = CustomDialog.createEditText(activity);
        edit.setHint(title);
        edit.setSingleLine(true);
        edit.setInputType(InputType.TYPE_CLASS_TEXT);

        var controls = new DateControls(activity, locale, filters.getOrDefault(playlistId, DateRange.EMPTY));
        String searchLabel = str("morphe_search_dialog_search");
        var dialogAndLayout = CustomDialog.create(activity, title, null, edit, searchLabel,
                () -> {
                    DateRange range = controls.dates;
                    if (!range.isValid()) return;
                    String query = edit.getText().toString().trim();
                    if (range.isEmpty()) {
                        filters.remove(playlistId);
                        if (!query.isEmpty()) originalSearch.accept(query);
                    } else {
                        filters.put(playlistId, range);
                        PlaylistDateResults.show(activity, locale, playlistId, query, range);
                    }
                }, () -> {}, null, null, true);
        Dialog dialog = dialogAndLayout.first;

        // Keep Morphe's scrolling container. Move its rounded field background onto the input
        // so only the original text field has the field background, even with the calendar below.
        LinearLayout content = (LinearLayout) edit.getParent();
        ScrollView scroll = (ScrollView) content.getParent();
        edit.setBackground(scroll.getBackground());
        int fieldPadding = dp(activity, 8);
        edit.setPadding(fieldPadding, fieldPadding, fieldPadding, fieldPadding);
        edit.setClipToOutline(true);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        scroll.setPadding(0, 0, 0, 0);
        scroll.setClipToOutline(false);
        content.addView(controls.layout, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ArrayList<View> buttons = new ArrayList<>();
        dialogAndLayout.second.findViewsWithText(buttons, searchLabel, View.FIND_VIEWS_WITH_TEXT);
        controls.search = buttons.stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> searchLabel.contentEquals(button.getText())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Original search action missing"));
        controls.refresh();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE |
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        edit.requestFocus();
        dialog.show();
    }

    private static final class DateControls {
        final Activity activity;
        final Locale locale;
        final DateTimeFormatter format;
        final LinearLayout layout;
        final Button from;
        final Button until;
        final Button clear;
        final TextView error;
        DateRange dates;
        Button search;

        DateControls(Activity activity, Locale locale, DateRange initialDates) {
            this.activity = activity;
            this.locale = locale;
            this.dates = initialDates;
            format = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale);
            layout = new LinearLayout(activity);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(0, dp(activity, 12), 0, 0);

            TextView heading = label(activity, phrase(locale, "Filtrer par période", "Filter by date"));
            heading.setTypeface(Typeface.DEFAULT_BOLD);
            layout.addView(heading);

            from = pickButton(true);
            until = pickButton(false);
            layout.addView(CustomDialog.createButtonRow(activity, from, until));

            clear = new Button(activity, null, android.R.attr.borderlessButtonStyle);
            clear.setAllCaps(false);
            clear.setTextSize(14);
            clear.setMinHeight(dp(activity, 48));
            clear.setTextColor(ThemeUtils.getDialogTextColor());
            clear.setText(phrase(locale, "Effacer les dates", "Clear dates"));
            clear.setOnClickListener(view -> {
                dates = DateRange.EMPTY;
                refresh();
            });
            layout.addView(clear, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            error = label(activity, phrase(locale,
                    "La date de début doit précéder ou égaler la date de fin.",
                    "The start date must be on or before the end date."));
            error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            layout.addView(error);
        }

        private Button pickButton(boolean start) {
            return CustomDialog.createButton(activity, null, "", () -> pick(start), false, false);
        }

        private void pick(boolean start) {
            LocalDate picked = start ? dates.start() : dates.end();
            LocalDate other = start ? dates.end() : dates.start();
            LocalDate initial = picked != null ? picked : other != null ? other : LocalDate.now();
            var input = activity.getSystemService(InputMethodManager.class);
            if (input != null) input.hideSoftInputFromWindow(from.getWindowToken(), 0);
            int theme = Utils.isDarkModeEnabled() ? android.R.style.Theme_Material_Dialog_Alert
                    : android.R.style.Theme_Material_Light_Dialog_Alert;
            var picker = new DatePickerDialog(activity, theme, (view, year, month, day) -> {
                LocalDate date = LocalDate.of(year, month + 1, day);
                dates = start ? new DateRange(date, dates.end()) : new DateRange(dates.start(), date);
                refresh();
            }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
            picker.setTitle(start ? phrase(locale, "Date de début", "Start date")
                    : phrase(locale, "Date de fin", "End date"));
            picker.show();
        }

        void refresh() {
            setDate(from, dates.start(), phrase(locale, "Du", "From"),
                    phrase(locale, "Date de début", "Start date"));
            setDate(until, dates.end(), phrase(locale, "Au", "To"),
                    phrase(locale, "Date de fin", "End date"));
            clear.setVisibility(dates.isEmpty() ? View.GONE : View.VISIBLE);
            error.setVisibility(dates.isValid() ? View.GONE : View.VISIBLE);
            search.setEnabled(dates.isValid());
            search.setAlpha(dates.isValid() ? 1f : 0.5f);
        }

        private void setDate(Button button, LocalDate date, String prefix, String label) {
            button.setText(date == null ? label : prefix + " " + format.format(date));
            button.setContentDescription(date == null ? label : label + ", " + format.format(date));
        }
    }

    static TextView label(Context context, String text) {
        var label = new TextView(context);
        label.setText(text);
        label.setTextSize(14);
        label.setTextColor(ThemeUtils.getDialogTextColor());
        return label;
    }

    static int dp(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    static String phrase(Locale locale, String french, String english) {
        return "fr".equals(locale.getLanguage()) ? french : english;
    }
}
