package fr.nesquik.playlistdate;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import fr.nesquik.playlistdate.WatchLaterDateIndex.Imported;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public final class WatchLaterDates extends SQLiteOpenHelper {
    public static final String PLAYLIST = "WL";
    private static final String CREATE_TAKEOUT = "CREATE TABLE takeout (scope TEXT NOT NULL, video TEXT NOT NULL,"
            + " added INTEGER NOT NULL, entry TEXT, PRIMARY KEY (scope, video))";
    private static WatchLaterDates instance;

    private WatchLaterDates(Context context) {
        super(context.getApplicationContext(), "nesquik-watch-later-dates.db", null, 2);
    }

    public static synchronized WatchLaterDates open(Context context) {
        if (instance == null) instance = new WatchLaterDates(context);
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE saved (entry TEXT PRIMARY KEY NOT NULL, video TEXT NOT NULL, added INTEGER NOT NULL)");
        db.execSQL(CREATE_TAKEOUT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) db.execSQL(CREATE_TAKEOUT);
    }

    static String scope(Map<String, String> headers) {
        return headers.getOrDefault("X-Goog-PageId", "");
    }

    int importDates(String scope, Map<String, Instant> dates) {
        var db = getWritableDatabase();
        db.beginTransaction();
        try {
            Map<String, Imported> previous = readTakeout(db, scope);
            db.delete("takeout", "scope = ?", new String[]{scope});
            for (var date : dates.entrySet()) {
                long added = date.getValue().toEpochMilli();
                ContentValues row = new ContentValues(4);
                row.put("scope", scope);
                row.put("video", date.getKey());
                row.put("added", added);
                Imported old = previous.get(date.getKey());
                if (old != null && old.added() == added) row.put("entry", old.entry());
                db.insertOrThrow("takeout", null, row);
            }
            db.setTransactionSuccessful();
            return dates.size();
        } finally {
            db.endTransaction();
        }
    }

    public void confirm(String video, String entry, Instant date) {
        if (video.isEmpty() || entry.isEmpty()) return;
        insert(getWritableDatabase(), video, entry, date);
    }

    private static void insert(SQLiteDatabase db, String video, String entry, Instant date) {
        ContentValues row = new ContentValues(3);
        row.put("entry", entry);
        row.put("video", video);
        row.put("added", date.toEpochMilli());
        db.insertWithOnConflict("saved", null, row, SQLiteDatabase.CONFLICT_IGNORE);
    }

    WatchLaterDateIndex snapshot(String scope) {
        var db = getReadableDatabase();
        Map<String, Long> confirmed = new HashMap<>();
        try (var cursor = db.query("saved", new String[]{"entry", "added", "video"},
                null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                confirmed.put(WatchLaterDateIndex.key(cursor.getString(2), cursor.getString(0)), cursor.getLong(1));
            }
        }
        return new WatchLaterDateIndex(confirmed, readTakeout(db, scope), ZoneId.systemDefault(),
                bindings -> saveBindings(scope, bindings));
    }

    private static Map<String, Imported> readTakeout(SQLiteDatabase db, String scope) {
        Map<String, Imported> results = new HashMap<>();
        try (var cursor = db.query("takeout", new String[]{"video", "added", "entry"},
                "scope = ?", new String[]{scope}, null, null, null)) {
            while (cursor.moveToNext()) {
                results.put(cursor.getString(0), new Imported(cursor.getLong(1), cursor.getString(2)));
            }
        }
        return results;
    }

    private void saveBindings(String scope, Map<String, Imported> bindings) {
        var db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues row = new ContentValues(1);
            for (var binding : bindings.entrySet()) {
                Imported date = binding.getValue();
                row.put("entry", date.entry());
                db.update("takeout", row, "scope = ? AND video = ? AND added = ? AND entry IS NULL",
                        new String[]{scope, binding.getKey(), Long.toString(date.added())});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
