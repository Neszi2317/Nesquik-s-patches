package fr.nesquik.detox;

import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import com.urbandroid.ddc.context.AppContext;
import com.urbandroid.ddc.model.Challenge;
import com.urbandroid.ddc.receiver.StartDetoxReceiver;
import com.urbandroid.ddc.service.ChallengeService;
import com.urbandroid.ddc.service.DdcAccessibilityService;

final class Engine {
  static Challenge current(Context c) {
    AppContext.initAll(c);
    return AppContext.settings().getCurrentChallenge();
  }

  static void state(Store s, Wire w, String value) {
    s.db.execSQL(
        "UPDATE inbox SET state=? WHERE peer=? AND id=?", new Object[] {value, w.sender, w.nonce});
  }

  /** Always called on the app's main looper, as are DDC's receiver and manual confirmation. */
  static void apply(Context c, Wire w, boolean visible) throws Exception {
    long now = System.currentTimeMillis();
    if (w.end <= now) return;
    Store s = Store.get(c);
    String st = s.one("SELECT state FROM inbox WHERE peer=? AND id=?", w.sender, w.nonce);
    if (st.isEmpty()) return;
    if (st.equals("attempt")) {
      // A process loss between dispatch and observation must never issue a second native start.
      observe(c, w, false);
      return;
    }
    if (!st.equals("received") && !st.equals("tap")) {
      Push.reply(c, w, st);
      return;
    }
    Challenge existing = current(c);
    if (existing != null || ChallengeService.isRunning()) {
      state(s, w, "busy");
      Push.reply(c, w, "busy");
      return;
    }
    boolean allowed =
        visible
            || Ui.isVisible()
            || Build.VERSION.SDK_INT < 31
            || c.getSystemService(PowerManager.class)
                .isIgnoringBatteryOptimizations(c.getPackageName());
    boolean permission =
        Settings.canDrawOverlays(c) || DdcAccessibilityService.isAccessibilityEnabled(c);
    if (!allowed || !permission) {
      state(s, w, "tap");
      Ui.notice(c, Ui.status(c, "tapLocal"), true);
      Push.reply(c, w, "tap");
      return;
    }
    long remaining = w.end - System.currentTimeMillis();
    if (remaining <= 0) return;
    state(s, w, "attempt");
    try {
      new StartDetoxReceiver()
          .onReceive(
              c,
              new Intent("com.urbandroid.ddc.START_DETOX")
                  .setPackage(c.getPackageName())
                  .putExtra("time_extra", remaining));
    } catch (RuntimeException ignored) {
      /* Native state may already have been persisted. */
    }
    Push.reply(c, w, "received");
    new Handler(Looper.getMainLooper())
        .postDelayed(
            () -> {
              try {
                observe(c, w, false);
              } catch (Exception ignored) {
              }
            },
            2000);
  }

  private static void observe(Context c, Wire w, boolean finalCheck) throws Exception {
    Store s = Store.get(c);
    if (!s.one("SELECT state FROM inbox WHERE peer=? AND id=?", w.sender, w.nonce)
        .equals("attempt")) return;
    Challenge ch = current(c);
    String result =
        (ch != null && ChallengeService.isRunning() && Math.abs(ch.getEndTime() - w.end) <= 3000)
            ? "active"
            : "uncertain";
    if (result.equals("uncertain") && !finalCheck) {
      Push.reply(c, w, "received");
      new Handler(Looper.getMainLooper())
          .postDelayed(
              () -> {
                try {
                  observe(c, w, true);
                } catch (Exception ignored) {
                }
              },
              10000);
      return;
    }
    state(s, w, result);
    Push.reply(c, w, result);
  }

  static void foreground(Activity a) {
    if (Build.VERSION.SDK_INT < 26) return;
    try {
      Store s = Store.get(a);
      for (String[] row :
          s.rows(
              "SELECT packet FROM inbox WHERE state IN ('tap','received','attempt') AND until>?"
                  + " ORDER BY until DESC",
              "" + System.currentTimeMillis())) apply(a, Wire.parse(row[0]), true);
      if (!s.one("SELECT id FROM peers LIMIT 1").isEmpty()) Delivery.kick(a);
    } catch (Exception ignored) {
      /* A sync problem must not block the native UI. */
    }
  }
}
