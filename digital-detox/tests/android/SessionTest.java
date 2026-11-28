package fr.nesquik.detox;

import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.CheckBox;
import com.urbandroid.ddc.model.Challenge;
import com.urbandroid.ddc.receiver.StartDetoxReceiver;
import com.urbandroid.ddc.service.ChallengeService;
import com.urbandroid.ddc.service.DdcAccessibilityService;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = 36,
    manifest = Config.NONE,
    instrumentedPackages = {"fr.nesquik.detox", "com.urbandroid.ddc"},
    shadows = {
      SessionTest.Keys.class,
      SessionTest.Network.class,
      SessionTest.Reader.class,
      SessionTest.Block.class,
      SessionTest.Access.class,
      SessionTest.Service.class
    })
public class SessionTest {
  @Implements(value = Identity.class, isInAndroidSdk = false)
  public static class Keys {
    @Implementation
    protected static KeyPair keys() {
      return local;
    }
  }

  @Implements(value = Delivery.class, isInAndroidSdk = false)
  public static class Network {
    @Implementation
    protected static void kick(Context c) {}
  }

  @Implements(value = Engine.class, isInAndroidSdk = false)
  public static class Reader {
    @Implementation
    protected static Challenge current(Context c) {
      return host;
    }
  }

  @Implements(value = StartDetoxReceiver.class, isInAndroidSdk = false)
  public static class Block {
    @Implementation
    protected void onReceive(Context c, Intent i) {
      assertEquals("com.urbandroid.ddc.START_DETOX", i.getAction());
      assertEquals(java.util.Collections.singleton("time_extra"), i.getExtras().keySet());
      starts++;
      long end =
          org.robolectric.shadows.ShadowSystem.currentTimeMillis()
              + i.getLongExtra("time_extra", -1);
      host = session(end);
      running = true;
    }
  }

  @Implements(value = DdcAccessibilityService.class, isInAndroidSdk = false)
  public static class Access {
    @Implementation
    protected static boolean isAccessibilityEnabled(Context c) {
      return permission;
    }
  }

  @Implements(value = ChallengeService.class, isInAndroidSdk = false)
  public static class Service {
    @Implementation
    protected static boolean isRunning() {
      return running;
    }
  }

  private static Challenge session(long end) {
    return new Challenge() {
      @Override
      public long getEndTime() {
        return end;
      }
    };
  }

  static KeyPair local, remote;
  static Challenge host;
  static boolean running, permission;
  static int starts;
  Context c;
  Store db;
  String self, peer, packet, id;
  long end;

  @Before
  public void setup() throws Exception {
    android.os.SystemClock.setCurrentTimeMillis(1795800000000L);
    KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
    g.initialize(new ECGenParameterSpec("secp256r1"));
    local = g.generateKeyPair();
    remote = g.generateKeyPair();
    self = Wire.id(local.getPublic());
    peer = Wire.id(remote.getPublic());
    java.lang.reflect.Field singleton = Store.class.getDeclaredField("instance");
    singleton.setAccessible(true);
    Store previous = (Store) singleton.get(null);
    if (previous != null && previous.db.isOpen()) previous.db.close();
    singleton.set(null,null);
    c = RuntimeEnvironment.getApplication();
    db = Store.get(c);
    for (String table : new String[] {"cfg", "peers", "inbox", "tx", "sent"})
      db.db.delete(table, null, null);
    db.set("token", "test-only-instance");
    db.db.execSQL(
        "INSERT INTO peers(id,code,name) VALUES(?,?,?)",
        new Object[] {
          peer, "DD1.upABCDEFGHIJKL." + Wire.enc(remote.getPublic().getEncoded()), "tablet"
        });
    permission = true;
    running = false;
    host = null;
    starts = 0;
    long now = org.robolectric.shadows.ShadowSystem.currentTimeMillis();
    end = now + 60000;
    id = Wire.nonce();
    packet = Wire.sign(remote, self, id, "start", now, end);
    ((org.robolectric.shadows.ShadowPowerManager)
            org.robolectric.shadow.api.Shadow.extract(c.getSystemService(PowerManager.class)))
        .setIgnoringBatteryOptimizations(c.getPackageName(), true);
  }

  private void deliver(String body, String token) {
    Intent i =
        new Intent(Push.CONNECT + "MESSAGE")
            .putExtra("bytesMessage", body.getBytes(StandardCharsets.US_ASCII))
            .putExtra("token", token);
    new Push().onReceive(c, i);
  }

  private void send() {
    deliver(packet, "test-only-instance");
  }

  private String state() {
    return db.one("SELECT state FROM inbox WHERE peer=? AND id=?", peer, id);
  }

  @Test
  public void duplicateAndProcessReopen() throws Exception {
    send();
    send();
    assertEquals(1, starts);
    assertEquals("active", state());
    assertTrue(Math.abs(host.getEndTime() - end) < 1500);
    assertEquals("0", db.one("SELECT count(*) FROM tx WHERE kind='start'"));
    db.db.close();
    java.lang.reflect.Field f = Store.class.getDeclaredField("instance");
    f.setAccessible(true);
    f.set(null, null);
    db = Store.get(c);
    send();
    assertEquals(1, starts);
    assertEquals("active", state());
  }

  @Test
  public void activeSessionKept() {
    host = session(end + 7200000);
    running = true;
    long before = host.getEndTime();
    send();
    assertEquals(0, starts);
    assertEquals(before, host.getEndTime());
    assertEquals("busy", state());
  }

  @Test
  public void deniedBackgroundThenForeground() throws Exception {
    ((org.robolectric.shadows.ShadowPowerManager)
            org.robolectric.shadow.api.Shadow.extract(c.getSystemService(PowerManager.class)))
        .setIgnoringBatteryOptimizations(c.getPackageName(), false);
    send();
    assertEquals("tap", state());
    assertEquals(0, starts);
    Engine.apply(c, Wire.parse(packet), true);
    assertEquals(1, starts);
    ShadowLooper.idleMainLooper(3, java.util.concurrent.TimeUnit.SECONDS);
    assertEquals("active", state());
  }

  @Test
  public void invalidExpiredAndRevoked() throws Exception {
    deliver(packet, "wrong-token");
    assertEquals("", state());
    deliver(packet.replace(peer, self), "test-only-instance");
    assertEquals("", state());
    long now = org.robolectric.shadows.ShadowSystem.currentTimeMillis();
    deliver(Wire.sign(remote, self, id, "start", now - 9000, now - 1), "test-only-instance");
    assertEquals("", state());
    db.remove(peer);
    send();
    assertEquals(0, starts);
    assertEquals("", state());
  }

  @Test
  public void missingPermissionsDoNotStart() {
    permission = false;
    send();
    assertEquals("tap", state());
    assertEquals(0, starts);
  }

  @Test
  public void manualUncheckedSendsNothing() {
    Activity a = Robolectric.buildActivity(Activity.class).setup().get();
    Dialog d = new Dialog(a);
    CheckBox option = new CheckBox(a);
    option.setTag("fr.nesquik.detox.sync.session");
    d.setContentView(option);
    d.show();
    Ui.begin(d);
    host = session(end);
    Ui.confirmed(c);
    assertEquals("0", db.one("SELECT count(*) FROM sent"));
    host = null;
    option.setChecked(true);
    Ui.begin(d);
    host = session(end);
    Ui.confirmed(c);
    assertEquals("1", db.one("SELECT count(*) FROM sent"));
    Ui.confirmed(c);
    assertEquals("1", db.one("SELECT count(*) FROM sent"));
  }
}
