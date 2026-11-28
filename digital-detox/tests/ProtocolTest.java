package fr.nesquik.detox;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.*;
import org.json.JSONObject;

/** Two ephemeral identities and a real local ntfy relay. No Android or production credentials. */
public class ProtocolTest {
  static void check(boolean ok, String name) {
    if (!ok) throw new AssertionError(name);
    System.out.println("PASS " + name);
  }

  static KeyPair keys() throws Exception {
    KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
    g.initialize(new ECGenParameterSpec("secp256r1"));
    return g.generateKeyPair();
  }

  static Wire.Peer peer(KeyPair k, String topic) throws Exception {
    return new Wire.Peer("DD1." + topic + "." + Wire.enc(k.getPublic().getEncoded()));
  }

  static String cached(String base, String topic) throws Exception {
    URL url = new URL(base + "/" + topic + "/json?poll=1&since=all");
    try (Scanner sc = new Scanner(url.openStream(), "UTF-8")) {
      String last = "";
      while (sc.hasNextLine()) {
        JSONObject obj = new JSONObject(sc.nextLine());
        if (obj.optString("event").equals("message")) {
          last = obj.getString("message");
          if (obj.optString("encoding").equals("base64"))
            last = new String(Base64.getDecoder().decode(last), StandardCharsets.US_ASCII);
        }
      }
      return last;
    }
  }

  public static void main(String[] args) throws Exception {
    KeyPair a = keys(), b = keys(), stranger = keys();
    String random = UUID.randomUUID().toString().replace("-", "");
    Wire.Peer pa = peer(a, "up" + random.substring(0, 12)),
        pb = peer(b, "up" + random.substring(12, 24));
    long now = System.currentTimeMillis(), end = now + 180000;
    String id = Wire.nonce(), cmd = Wire.sign(a, pb.id, id, "start", now, end);
    check(Wire.parse(cmd).valid(pa, pb.id, now), "A to B authenticated");
    check(!Wire.parse(cmd).valid(pa, pa.id, now), "reflection and wrong recipient rejected");
    check(!Wire.parse(cmd).valid(peer(stranger, pa.topic), pb.id, now), "unknown signer rejected");
    check(
        !Wire.parse(cmd.replace("." + end + ".", "." + (end + 1) + ".")).valid(pa, pb.id, now),
        "modified deadline rejected");
    check(!Wire.parse(cmd).valid(pa, pb.id, end), "expired start rejected");
    check(!Wire.parse(cmd).valid(pa, pb.id, now - 61000), "future command rejected");
    for (String verb : new String[] {"stop", "shorten", "extend", "whitelist"}) {
      boolean rejected = false;
      try {
        Wire.parse(Wire.sign(a, pb.id, id, verb, now, end));
      } catch (IllegalArgumentException expected) {
        rejected = true;
      }
      check(rejected, "unsupported " + verb);
    }
    String base = args[0];
    check(
        Delivery.post(new URL(base + "/" + pb.topic + "?up=1"), cmd) == 200,
        "HTTPS publish via production sender");
    String late = cached(base, pb.topic);
    check(
        late.equals(cmd)
            && Wire.parse(late).valid(pa, pb.id, now + 90000)
            && Wire.parse(late).end - (now + 90000) == 90000,
        "offline catch-up and remaining duration");
    String ack = Wire.sign(b, pa.id, id, "active", now + 1, end);
    check(
        Delivery.post(new URL(base + "/" + pa.topic + "?up=1"), ack) == 200
            && Wire.parse(cached(base, pa.topic)).valid(pb, pa.id, now + 1),
        "return ACK over HTTPS");
    String back = Wire.sign(b, pa.id, Wire.nonce(), "start", now, end);
    check(
        Delivery.post(new URL(base + "/" + pa.topic + "?up=1"), back) == 200
            && cached(base, pa.topic).equals(back)
            && Wire.parse(back).valid(pb, pa.id, now),
        "B to A start over HTTPS");
  }
}
