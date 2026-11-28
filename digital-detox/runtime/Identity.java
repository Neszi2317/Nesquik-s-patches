package fr.nesquik.detox;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.security.*;
import java.security.spec.ECGenParameterSpec;

final class Identity {
  private static final String ALIAS = "nesquik.ddc.sync.v1";

  static synchronized KeyPair keys() throws GeneralSecurityException, java.io.IOException {
    KeyStore store = KeyStore.getInstance("AndroidKeyStore");
    store.load(null);
    if (!store.containsAlias(ALIAS)) {
      KeyPairGenerator g = KeyPairGenerator.getInstance("EC", "AndroidKeyStore");
      g.initialize(
          new KeyGenParameterSpec.Builder(
                  ALIAS, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
              .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
              .setDigests(KeyProperties.DIGEST_SHA256)
              .build());
      return g.generateKeyPair();
    }
    return new KeyPair(
        store.getCertificate(ALIAS).getPublicKey(), (PrivateKey) store.getKey(ALIAS, null));
  }
}
