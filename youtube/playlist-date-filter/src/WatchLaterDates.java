package fr.nesquik.playlistdate;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public final class WatchLaterDates extends SQLiteOpenHelper {
    public static final String PLAYLIST = "WL";
    private static WatchLaterDates instance;

    private WatchLaterDates(Context context) {
        super(context.getApplicationContext(), "nesquik-watch-later-dates.db", null, 1);
    }

    public static synchronized WatchLaterDates open(Context context) {
        if (instance == null) instance = new WatchLaterDates(context);
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE saved (entry TEXT PRIMARY KEY NOT NULL, video TEXT NOT NULL, added INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Unexpected Watch later database version");
    }

    int importDates(Map<String, Instant> dates, Map<String, String> watchedLater) {
        var db = getWritableDatabase();
        db.beginTransaction();
        try {
            int matched = 0;
            for (var row : dates.entrySet()) {
                String entry = watchedLater.get(row.getKey());
                if (entry == null || entry.isEmpty()) continue;
                insert(db, row.getKey(), entry, row.getValue());
                matched++;
            }
            db.setTransactionSuccessful();
            return matched;
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

    public static String key(String video, String entry) {
        return entry + ":" + video;
    }

    public Map<String, LocalDate> snapshot() {
        Map<String, LocalDate> results = new HashMap<>();
        ZoneId zone = ZoneId.systemDefault();
        try (var cursor = getReadableDatabase().query("saved", new String[]{"entry", "added", "video"},
                null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                LocalDate date = Instant.ofEpochMilli(cursor.getLong(1)).atZone(zone).toLocalDate();
                results.put(key(cursor.getString(2), cursor.getString(0)), date);
            }
        }
        return results;
    }
}
