package fr.nesquik.detox;

import android.app.*;
import android.content.*;
import android.os.*;
import android.text.InputType;
import android.widget.*;
import java.text.DateFormat;
import java.util.Date;

public final class Devices extends Activity {
  private Store store;
  private LinearLayout list;
  private String lastDisplay;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable refresh =
      new Runnable() {
        public void run() {
          render();
          handler.postDelayed(this, 2000);
        }
      };

  private String t(String en, String fr) {
    return Ui.t(this, en, fr);
  }

  public void onCreate(Bundle state) {
    super.onCreate(state);
    setTitle(Ui.title(this));
    store = Store.get(this);
    ScrollView scroll = new ScrollView(this);
    list = new LinearLayout(this);
    list.setOrientation(LinearLayout.VERTICAL);
    int p = (int) (16 * getResources().getDisplayMetrics().density);
    list.setPadding(p, p, p, p);
    scroll.addView(list);
    setContentView(scroll);
    if (Build.VERSION.SDK_INT >= 33
        && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        && store.get("notificationAsked").isEmpty()) {
      store.set("notificationAsked", "1");
      requestPermissions(new String[] {android.Manifest.permission.POST_NOTIFICATIONS}, 73);
    }
    if (Build.VERSION.SDK_INT >= 26) {
      try {
        Push.register(this);
      } catch (RuntimeException ignored) {
      }
    }
  }

  protected void onResume() {
    super.onResume();
    refresh.run();
  }

  protected void onPause() {
    handler.removeCallbacks(refresh);
    super.onPause();
  }

  private void label(String s) {
    TextView v = new TextView(this);
    v.setText(s);
    list.addView(v);
  }

  private void button(String s, Runnable action) {
    Button b = new Button(this);
    b.setText(s);
    b.setAllCaps(false);
    b.setOnClickListener(v -> action.run());
    list.addView(b);
  }

  private void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_LONG).show();
  }

  private void render() {
    if (Build.VERSION.SDK_INT < 26) {
      list.removeAllViews();
      label(t("Android 8 or newer required", "Android 8 ou plus récent requis"));
      return;
    }
    boolean ready = !store.get("topic").isEmpty();
    boolean exempt =
        getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName());
    long now = System.currentTimeMillis();
    java.util.List<String[]> peers = store.rows("SELECT id,name,last FROM peers ORDER BY name");
    java.util.List<String[]> requests = new java.util.ArrayList<>();
    StringBuilder snapshot =
        new StringBuilder().append(ready).append(exempt).append(store.get("default"));
    for (String[] peer : peers) {
      java.util.List<String[]> found =
          store.rows(
              "SELECT state,until FROM sent WHERE peer=? ORDER BY rowid DESC LIMIT 1", peer[0]);
      String[] req = found.isEmpty() ? null : found.get(0);
      requests.add(req);
      snapshot.append(java.util.Arrays.toString(peer)).append(java.util.Arrays.toString(req));
      if (req != null) snapshot.append(Long.parseLong(req[1]) <= now);
    }
    if (snapshot.toString().equals(lastDisplay)) return;
    lastDisplay = snapshot.toString();
    list.removeAllViews();
    if (!exempt)
      label(
          t(
              "For automatic starts while asleep, exclude Digital Detox from battery optimization.",
              "Pour démarrer automatiquement en veille, exclus Digital Detox de l’optimisation de"
                  + " la batterie."));
    if (!ready) {
      label(
          t(
              "Install ntfy, open it and enable UnifiedPush. Use ntfy.sh as its default server.",
              "Installe ntfy, ouvre-la et active UnifiedPush. Utilise ntfy.sh comme serveur par"
                  + " défaut."));
      button(t("Connect to ntfy", "Connecter à ntfy"), () -> Push.register(this));
    } else {
      button(t("Show my pairing code", "Afficher mon code d’association"), this::showCode);
      button(
          t("Add a device", "Associer un appareil"),
          () -> input(t("Other device’s code", "Code de l’autre appareil"), "", this::pair));
    }
    Switch def = new Switch(this);
    def.setText(t("Synchronize by default", "Synchroniser par défaut"));
    def.setChecked(store.get("default").equals("1"));
    def.setOnCheckedChangeListener((b, v) -> store.set("default", v ? "1" : "0"));
    list.addView(def);
    for (int index = 0; index < peers.size(); index++) {
      String[] peer = peers.get(index);
      String id = peer[0];
      long last = Long.parseLong(peer[2]);
      String desc =
          last == 0
              ? t("No authenticated response yet", "Aucune réponse authentifiée")
              : t("Last contact ", "Dernier contact ")
                  + DateFormat.getDateTimeInstance().format(new Date(last));
      String[] req = requests.get(index);
      if (req != null) {
        String status = req[0];
        if (Long.parseLong(req[1]) <= System.currentTimeMillis()
            && !status.matches("active|busy|pong")) status = "expired";
        desc += "\n" + Ui.status(this, status);
      }
      button(peer[1] + "\n" + desc, () -> manage(id, peer[1]));
    }
  }

  private void showCode() {
    try {
      String code =
          "DD1." + store.get("topic") + "." + Wire.enc(Identity.keys().getPublic().getEncoded());
      TextView v = new TextView(this);
      v.setPadding(24, 12, 24, 12);
      v.setTextIsSelectable(true);
      v.setText(code);
      new AlertDialog.Builder(this)
          .setTitle(t("Enter on the other device", "À saisir sur l’autre appareil"))
          .setView(v)
          .setPositiveButton(
              t("Copy", "Copier"),
              (d, w) ->
                  getSystemService(ClipboardManager.class)
                      .setPrimaryClip(ClipData.newPlainText("Digital Detox", code)))
          .setNegativeButton(android.R.string.cancel, null)
          .show();
    } catch (Exception e) {
      toast(t("Identity unavailable", "Identité indisponible"));
    }
  }

  private interface Value {
    void accept(String text);
  }

  private void input(String title, String initial, Value action) {
    EditText e = new EditText(this);
    e.setSingleLine();
    e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    e.setText(initial);
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setView(e)
        .setPositiveButton(
            android.R.string.ok, (d, w) -> action.accept(e.getText().toString().trim()))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void pair(String code) {
    try {
      Wire.Peer peer = new Wire.Peer(code);
      if (peer.id.equals(Wire.id(Identity.keys().getPublic())))
        throw new IllegalArgumentException();
      String name = t("Device ", "Appareil ") + peer.id.substring(0, 8);
      store.db.execSQL(
          "INSERT OR IGNORE INTO peers(id,code,name) VALUES(?,?,?)",
          new Object[] {peer.id, code, name});
      store.db.execSQL("UPDATE peers SET code=? WHERE id=?", new Object[] {code, peer.id});
      Delivery.schedule(this);
      toast(
          t(
              "Add this device’s code on the other device as well.",
              "Ajoute aussi le code de cet appareil sur l’autre."));
      render();
    } catch (Exception e) {
      toast(t("Invalid pairing code", "Code d’association invalide"));
    }
  }

  private void manage(String id, String name) {
    String[] actions = {
      t("Check connection", "Vérifier la connexion"),
      t("Rename", "Renommer"),
      t("Unpair", "Dissocier")
    };
    new AlertDialog.Builder(this)
        .setTitle(name)
        .setItems(
            actions,
            (d, w) -> {
              if (w == 0) {
                try {
                  Delivery.request(this, id, "ping", System.currentTimeMillis() + 120000);
                } catch (Exception e) {
                  toast(Ui.status(this, "uncertain"));
                }
              }
              if (w == 1)
                input(
                    actions[1],
                    name,
                    value -> {
                      if (!value.isEmpty())
                        store.db.execSQL(
                            "UPDATE peers SET name=? WHERE id=?",
                            new Object[] {value.substring(0, Math.min(60, value.length())), id});
                    });
              if (w == 2)
                new AlertDialog.Builder(this)
                    .setMessage(t("Remove this device?", "Dissocier cet appareil ?"))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(
                        android.R.string.ok,
                        (a, b) -> {
                          store.remove(id);
                          render();
                        })
                    .show();
            })
        .show();
  }
}
