package fr.nesquik.detox;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Only the main app process opens this database. SQLite serializes the network and UI threads. */
final class Store {
  private static Store instance;
  final SQLiteDatabase db;

  private Store(Context c) {
    db =
        SQLiteDatabase.openOrCreateDatabase(
            new File(c.getNoBackupFilesDir(), "nesquik-detox-v1.db"), null);
    db.execSQL("CREATE TABLE IF NOT EXISTS cfg(k TEXT PRIMARY KEY,v TEXT NOT NULL)");
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS peers(id TEXT PRIMARY KEY,code TEXT NOT NULL,name TEXT NOT"
            + " NULL,last INTEGER DEFAULT 0,status TEXT DEFAULT '')");
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS inbox(peer TEXT,id TEXT,packet TEXT,until INTEGER,state"
            + " TEXT,PRIMARY KEY(peer,id))");
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS tx(peer TEXT,id TEXT,kind TEXT,packet TEXT,until INTEGER,next"
            + " INTEGER,PRIMARY KEY(peer,id,kind))");
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS sent(peer TEXT,id TEXT,until INTEGER,state TEXT,PRIMARY"
            + " KEY(peer,id))");
  }

  static synchronized Store get(Context c) {
    if (instance == null) instance = new Store(c.getApplicationContext());
    return instance;
  }

  List<String[]> rows(String sql, String... args) {
    List<String[]> list = new ArrayList<>();
    try (Cursor cur = db.rawQuery(sql, args)) {
      while (cur.moveToNext()) {
        String[] row = new String[cur.getColumnCount()];
        for (int i = 0; i < row.length; i++) row[i] = cur.getString(i);
        list.add(row);
      }
    }
    return list;
  }

  String one(String sql, String... args) {
    List<String[]> found = rows(sql, args);
    return found.isEmpty() ? "" : found.get(0)[0];
  }

  String get(String k) {
    return one("SELECT v FROM cfg WHERE k=?", k);
  }

  void set(String k, String v) {
    db.execSQL("INSERT OR REPLACE INTO cfg VALUES(?,?)", new Object[] {k, v});
  }

  void remove(String id) {
    db.beginTransaction();
    try {
      for (String t : new String[] {"tx", "sent", "inbox"})
        db.delete(t, "peer=?", new String[] {id});
      db.delete("peers", "id=?", new String[] {id});
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }
}
