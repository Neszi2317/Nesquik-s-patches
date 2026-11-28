package fr.nesquik.detox;

import android.app.*;
import android.content.*;
import android.os.Build;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

public final class Push extends BroadcastReceiver {
  static final String CONNECT = "org.unifiedpush.android.connector.";
  static final String NTFY = "io.heckel.ntfy";

  static void register(Context c) {
    Store s = Store.get(c);
    String token = s.get("token");
    if (token.isEmpty()) {
      token = Wire.nonce() + Wire.nonce();
      s.set("token", token);
    }
    Intent i =
        new Intent("org.unifiedpush.android.distributor.REGISTER")
            .setPackage(NTFY)
            .putExtra("token", token)
            .putExtra("application", c.getPackageName());
    Intent inert = new Intent(c, Push.class).setAction("fr.nesquik.detox.IDENTITY");
    i.putExtra("pi", PendingIntent.getBroadcast(c, 11, inert, PendingIntent.FLAG_IMMUTABLE));
    if (Build.VERSION.SDK_INT >= 34)
      c.sendBroadcast(
          i, null, BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle());
    else c.sendBroadcast(i);
  }

  @Override
  public void onReceive(Context c, Intent i) {
    if (Build.VERSION.SDK_INT < 26
        || i == null
        || i.getAction() == null
        || !i.getAction().startsWith(CONNECT)) return;
    try {
      Store s = Store.get(c);
      String token = s.get("token");
      if (token.isEmpty() || !token.equals(i.getStringExtra("token"))) return;
      String action = i.getAction().substring(CONNECT.length());
      if (action.equals("NEW_ENDPOINT")) {
        s.set("topic", Wire.topic(i.getStringExtra("endpoint")));
        s.set("registration", "ok");
        return;
      }
      if (action.equals("UNREGISTERED") || action.equals("REGISTRATION_FAILED")) {
        s.set("registration", "failed");
        s.set("topic", "");
        return;
      }
      if (!action.equals("MESSAGE")) return;
      byte[] b = i.getByteArrayExtra("bytesMessage");
      if (b == null || b.length > 700) return;
      accept(c, new String(b, StandardCharsets.US_ASCII));
    } catch (Exception ignored) {
      /* Reject malformed, unknown, corrupt or unsigned commands. */
    }
  }

  static void accept(Context c, String packet) throws Exception {
    Wire w = Wire.parse(packet);
    Store s = Store.get(c);
    String code = s.one("SELECT code FROM peers WHERE id=?", w.sender);
    if (code.isEmpty()) return;
    KeyPair keys = Identity.keys();
    long now = System.currentTimeMillis();
    if (!w.valid(new Wire.Peer(code), Wire.id(keys.getPublic()), now)) return;
    s.db.execSQL("UPDATE peers SET last=? WHERE id=?", new Object[] {now, w.sender});
    if (w.type.equals("ping")) {
      reply(c, w, "pong");
      return;
    }
    if (w.type.equals("start")) {
      s.db.execSQL(
          "INSERT OR IGNORE INTO inbox VALUES(?,?,?,?,?)",
          new Object[] {w.sender, w.nonce, packet, w.end, "received"});
      String original = s.one("SELECT packet FROM inbox WHERE peer=? AND id=?", w.sender, w.nonce);
      if (!original.equals(packet)) return;
      Engine.apply(c, w, false);
      return;
    }
    String state =
        s.one(
            "SELECT state FROM sent WHERE peer=? AND id=? AND until=?",
            w.sender,
            w.nonce,
            "" + w.end);
    if (state.isEmpty() || state.equals("active") || state.equals("busy") || state.equals("pong"))
      return;
    if (w.type.equals("pong") != state.equals("ping")) return;
    if (state.equals("tap") && w.type.equals("received")) return;
    s.db.execSQL(
        "UPDATE sent SET state=? WHERE peer=? AND id=?", new Object[] {w.type, w.sender, w.nonce});
    // A 'tap' or 'received' receipt is nonterminal. Retrying allows a lost final ACK to be
    // recovered.
    if (w.type.equals("active") || w.type.equals("busy") || w.type.equals("pong"))
      s.db.delete("tx", "peer=? AND id=?", new String[] {w.sender, w.nonce});
    if (!w.type.equals("pong")) Ui.notice(c, Ui.status(c, w.type), false);
  }

  static void reply(Context c, Wire w, String type) throws Exception {
    long now = System.currentTimeMillis();
    if (now >= w.end) return;
    Store s = Store.get(c);
    Delivery.enqueue(
        s,
        w.sender,
        w.nonce,
        "ack",
        Wire.sign(Identity.keys(), w.sender, w.nonce, type, now, w.end),
        w.end);
    Delivery.kick(c);
  }
}
