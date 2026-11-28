package fr.nesquik.detox;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/** Every signed field has an unambiguous ASCII representation. All times are UTC epoch ms. */
final class Wire {
  static final long MAX_SPAN = 90L * 24 * 3600000;
  private static final String ID = "[A-Za-z0-9_-]{43}";
  private static final String TOPIC = "up[A-Za-z0-9]{12}";
  static final String ROOT = "https://ntfy.sh/";

  static String enc(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  static byte[] dec(String value) {
    return Base64.getUrlDecoder().decode(value);
  }

  static String id(PublicKey key) throws GeneralSecurityException {
    return enc(MessageDigest.getInstance("SHA-256").digest(key.getEncoded()));
  }

  static String topic(String endpoint) {
    if (endpoint == null || !endpoint.matches("https://ntfy\\.sh/" + TOPIC + "\\?up=1"))
      throw new IllegalArgumentException("endpoint");
    return endpoint.substring(ROOT.length(), endpoint.indexOf('?'));
  }

  static final class Peer {
    final String topic, id;
    final PublicKey key;

    Peer(String code) throws GeneralSecurityException {
      if (code.length() > 250) throw new IllegalArgumentException("length");
      String[] parts = code.trim().split("\\.", -1);
      if (parts.length != 3 || !parts[0].equals("DD1") || !parts[1].matches(TOPIC))
        throw new IllegalArgumentException("code");
      topic = parts[1];
      key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(dec(parts[2])));
      if (!(key instanceof ECPublicKey)
          || ((ECPublicKey) key).getParams().getCurve().getField().getFieldSize() != 256)
        throw new IllegalArgumentException("curve");
      id = id(key);
    }
  }

  final String sender, recipient, nonce, type;
  final long issued, end;
  private final String body;
  private final byte[] signature;

  private Wire(String packet) {
    if (packet.length() > 700) throw new IllegalArgumentException("length");
    String[] fields = packet.split("\\.", -1);
    if (fields.length != 8
        || !fields[0].equals("D1")
        || !fields[1].matches(ID)
        || !fields[2].matches(ID)
        || !fields[3].matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        || !fields[4].matches("start|ping|pong|received|active|busy|tap|uncertain")
        || !fields[5].matches("[0-9]{1,14}")
        || !fields[6].matches("[0-9]{1,14}")) throw new IllegalArgumentException("packet");
    sender = fields[1];
    recipient = fields[2];
    nonce = fields[3];
    type = fields[4];
    issued = Long.parseLong(fields[5]);
    end = Long.parseLong(fields[6]);
    body = packet.substring(0, packet.lastIndexOf('.'));
    signature = dec(fields[7]);
  }

  static Wire parse(String packet) {
    return new Wire(packet);
  }

  static String sign(KeyPair pair, String to, String nonce, String type, long now, long end)
      throws GeneralSecurityException {
    String body =
        String.join(
            ".",
            "D1",
            id(pair.getPublic()),
            to,
            nonce,
            type,
            Long.toString(now),
            Long.toString(end));
    Signature s = Signature.getInstance("SHA256withECDSA");
    s.initSign(pair.getPrivate());
    s.update(body.getBytes(StandardCharsets.US_ASCII));
    return body + "." + enc(s.sign());
  }

  boolean valid(Peer peer, String local, long now) throws GeneralSecurityException {
    if (!recipient.equals(local)
        || !sender.equals(peer.id)
        || sender.equals(local)
        || issued > now + 60000
        || end <= now
        || end <= issued
        || end - issued > MAX_SPAN) return false;
    Signature s = Signature.getInstance("SHA256withECDSA");
    s.initVerify(peer.key);
    s.update(body.getBytes(StandardCharsets.US_ASCII));
    return s.verify(signature);
  }

  static String nonce() {
    return UUID.randomUUID().toString();
  }
}
