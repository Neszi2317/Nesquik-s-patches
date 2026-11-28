package fr.nesquik.detox;

import android.app.job.*;
import android.content.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.HttpsURLConnection;

/** The durable queue owns retries. Relay acceptance and device acknowledgement are separate. */
public final class Delivery extends JobService {
  private static final int JOB = 0x44445331;
  private static final ExecutorService IO = Executors.newSingleThreadExecutor();
  private static boolean busy, dirty;
  private static final int RETRY = JOB + 1;

  static void schedule(Context c) {
    JobScheduler jobs = c.getSystemService(JobScheduler.class);
    if (jobs.getPendingJob(JOB) == null)
      jobs.schedule(
          new JobInfo.Builder(JOB, new ComponentName(c, Delivery.class))
              .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
              .setPersisted(true)
              .setPeriodic(15 * 60000L)
              .build());
  }

  static synchronized void kick(Context c) {
    dirty = true;
    if (busy) return;
    busy = true;
    Context app = c.getApplicationContext();
    IO.execute(
        () -> {
          while (true) {
            synchronized (Delivery.class) {
              if (!dirty) {
                busy = false;
                return;
              }
              dirty = false;
            }
            if (flush(app)) scheduleRetry(app);
          }
        });
  }

  private static void scheduleRetry(Context c) {
    JobScheduler jobs = c.getSystemService(JobScheduler.class);
    if (jobs.getPendingJob(RETRY) == null)
      jobs.schedule(
          new JobInfo.Builder(RETRY, new ComponentName(c, Delivery.class))
              .setPersisted(true)
              .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
              .setMinimumLatency(30000)
              .setBackoffCriteria(30000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
              .build());
  }

  public boolean onStartJob(JobParameters p) {
    IO.execute(
        () -> {
          boolean failed = flush(this);
          if (failed && p.getJobId() != RETRY) scheduleRetry(this);
          jobFinished(p, failed && p.getJobId() == RETRY);
        });
    return true;
  }

  public boolean onStopJob(JobParameters p) {
    return true;
  }

  static void enqueue(Store s, String peer, String id, String kind, String packet, long end) {
    s.db.execSQL(
        "INSERT OR REPLACE INTO tx VALUES(?,?,?,?,?,0)",
        new Object[] {peer, id, kind, packet, end});
  }

  static void request(Context c, String peer, String type, long end) throws Exception {
    Store s = Store.get(c);
    long now = System.currentTimeMillis();
    String id = Wire.nonce(), packet = Wire.sign(Identity.keys(), peer, id, type, now, end);
    s.db.beginTransaction();
    try {
      s.db.execSQL(
          "INSERT INTO sent VALUES(?,?,?,?)",
          new Object[] {peer, id, end, type.equals("ping") ? "ping" : "waiting"});
      enqueue(s, peer, id, type, packet, end);
      s.db.setTransactionSuccessful();
    } finally {
      s.db.endTransaction();
    }
    schedule(c);
    kick(c);
  }

  static int post(URL endpoint, String payload) throws IOException {
    HttpsURLConnection h = (HttpsURLConnection) endpoint.openConnection();
    try {
      h.setInstanceFollowRedirects(false);
      h.setConnectTimeout(5000);
      h.setReadTimeout(5000);
      h.setRequestMethod("POST");
      h.setDoOutput(true);
      h.setRequestProperty("Content-Type", "application/octet-stream");
      byte[] bytes = payload.getBytes(StandardCharsets.US_ASCII);
      h.setFixedLengthStreamingMode(bytes.length);
      try (OutputStream out = h.getOutputStream()) {
        out.write(bytes);
      }
      return h.getResponseCode();
    } finally {
      h.disconnect();
    }
  }

  private static boolean flush(Context c) {
    boolean failed = false;
    try {
      Store s = Store.get(c);
      long now = System.currentTimeMillis();
      String stamp = Long.toString(now);
      s.db.delete("tx", "until<=?", new String[] {stamp});
      s.db.delete("inbox", "until<=?", new String[] {stamp});
      s.db.delete("sent", "until<?", new String[] {Long.toString(now - 86400000L)});
      for (String[] r :
          s.rows(
              "SELECT tx.peer,tx.id,kind,packet,code FROM tx JOIN peers ON peers.id=tx.peer WHERE"
                  + " next<=? AND until>? ORDER BY next LIMIT 32",
              stamp,
              stamp)) {
        String[] pk = {r[0], r[1], r[2], r[3]};
        boolean accepted = false;
        try {
          String topic = new Wire.Peer(r[4]).topic;
          int status = post(new URL(Wire.ROOT + topic + "?up=1"), r[3]);
          accepted = status >= 200 && status < 300;
          if (accepted && !r[2].equals("start") && !r[2].equals("ping")) {
            s.db.delete("tx", "peer=? AND id=? AND kind=? AND packet=?", pk);
            continue;
          }
        } catch (Exception ignored) {
          /* A subsequent job retries this packet. */
        }
        if (!accepted) failed = true;
        s.db.execSQL(
            "UPDATE tx SET next=? WHERE peer=? AND id=? AND kind=? AND packet=?",
            new Object[] {
              System.currentTimeMillis() + (accepted ? 15 * 60000L : 20000L), r[0], r[1], r[2], r[3]
            });
      }
    } catch (RuntimeException ignored) {
      return true;
    }
    return failed;
  }
}
