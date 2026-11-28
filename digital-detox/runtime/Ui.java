package fr.nesquik.detox;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.widget.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.urbandroid.ddc.model.Challenge;
import java.lang.ref.WeakReference;
import java.util.Locale;

public final class Ui {
  private static final String OPTION = "fr.nesquik.detox.sync.session";
  private static boolean selected;
  private static WeakReference<Activity> visible = new WeakReference<>(null);

  static boolean isVisible() {
    Activity a = visible.get();
    return a != null && !a.isFinishing() && a.hasWindowFocus();
  }

  public static void begin(DialogInterface confirmation) {
    selected = false;
    if (!(confirmation instanceof Dialog)) return;
    android.view.Window window = ((Dialog) confirmation).getWindow();
    if (window == null) return;
    android.view.View choice = window.getDecorView().findViewWithTag(OPTION);
    if (choice instanceof CheckBox && ((CheckBox) choice).isChecked() && choice.isEnabled()) {
      try {
        selected =
            Engine.current(choice.getContext()) == null
                && !com.urbandroid.ddc.service.ChallengeService.isRunning();
      } catch (RuntimeException ignored) {
        selected = false;
      }
    }
  }

  static String t(Context c, String en, String fr) {
    Locale locale = c.getResources().getConfiguration().getLocales().get(0);
    return locale.getLanguage().equals("fr") ? fr : en;
  }

  static String title(Context c) {
    return t(c, "Device synchronization", "Synchronisation des appareils");
  }

  static String status(Context c, String state) {
    switch (state) {
      case "active":
        return t(c, "Remote session confirmed", "Session distante confirmée");
      case "busy":
        return t(
            c,
            "The other device already has a session",
            "Une détox est déjà en cours sur l’autre appareil");
      case "received":
        return t(
            c, "Command received; checking session", "Commande reçue, démarrage en vérification");
      case "uncertain":
        return t(
            c, "Native start could not be confirmed", "Démarrage natif impossible à confirmer");
      case "tap":
        return t(
            c,
            "Open Digital Detox on the other device",
            "Ouvre Digital Detox sur l’autre appareil");
      case "tapLocal":
        return t(
            c,
            "Open Digital Detox to start the remaining time. Check blocking permissions.",
            "Ouvre Digital Detox pour lancer le temps restant. Vérifie les autorisations de"
                + " blocage.");
      case "pong":
        return t(c, "Device replied", "L’appareil a répondu");
      case "expired":
        return t(c, "Expired without confirmation", "Échéance passée sans confirmation");
      case "ping":
        return t(c, "Checking connection…", "Vérification de la connexion…");
      default:
        return t(
            c,
            "Remote device has not confirmed receipt",
            "Réception non confirmée par l’autre appareil");
    }
  }

  public static void launchOption(MaterialAlertDialogBuilder builder) {
    if (Build.VERSION.SDK_INT < 26) return;
    try {
      Context c = builder.getContext();
      Store s = Store.get(c);
      CheckBox opt = new CheckBox(c);
      opt.setText(
          t(c, "Synchronize with my other devices", "Synchroniser avec mes autres appareils"));
      boolean paired = !s.one("SELECT id FROM peers LIMIT 1").isEmpty();
      opt.setEnabled(paired);
      opt.setChecked(paired && s.get("default").equals("1"));
      int margin = (int) (20 * c.getResources().getDisplayMetrics().density);
      LinearLayout container = new LinearLayout(c);
      container.setPadding(margin, 0, margin, 0);
      opt.setTag(OPTION);
      container.addView(opt);
      builder.setView(container);
    } catch (RuntimeException ignored) {
    }
  }

  // Both native launch branches call this only after their original start method has returned.
  public static void confirmed(Context context) {
    if (!selected) return;
    selected = false;
    Context c = context.getApplicationContext();
    try {
      Challenge ch = Engine.current(c);
      if (ch == null) return;
      long end = ch.getEndTime(), now = System.currentTimeMillis();
      if (end <= now || end - now > Wire.MAX_SPAN) {
        notice(
            c, t(c, "Duration outside sync range", "Durée hors plage de synchronisation"), false);
        return;
      }
      for (String[] peer : Store.get(c).rows("SELECT id FROM peers"))
        Delivery.request(c, peer[0], "start", end);
      notice(c, status(c, "waiting"), false);
    } catch (Exception ignored) {
      notice(c, status(c, "uncertain"), false);
    }
  }

  public static void resume(Activity a) {
    visible = new WeakReference<>(a);
    Engine.foreground(a);
  }

  static void notice(Context c, String text, boolean actionable) {
    if (Build.VERSION.SDK_INT < 26) return;
    try {
      NotificationManager nm = c.getSystemService(NotificationManager.class);
      String channel = "nesquik-detox-sync";
      nm.createNotificationChannel(
          new NotificationChannel(channel, title(c), NotificationManager.IMPORTANCE_DEFAULT));
      Intent open = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
      if (open == null) return;
      PendingIntent pi =
          PendingIntent.getActivity(
              c,
              actionable ? 10 : 11,
              open,
              PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
      Notification n =
          new Notification.Builder(c, channel)
              .setSmallIcon(android.R.drawable.ic_popup_sync)
              .setContentTitle(title(c))
              .setContentText(text)
              .setStyle(new Notification.BigTextStyle().bigText(text))
              .setContentIntent(pi)
              .setAutoCancel(true)
              .build();
      nm.notify(actionable ? 0x444401 : 0x444402, n);
    } catch (RuntimeException ignored) {
    }
  }
}
