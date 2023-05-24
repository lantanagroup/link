package com.lantanagroup.link;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * Used for creating a hash of (for example) passwords.
 * SALT is re-used so that the same hash can be created multiple times as long as the value is the same.
 */
public class Hasher {
  public static final String SALT = "982349234";

  public static String hash(String value) throws InvalidKeySpecException, NoSuchAlgorithmException {
    KeySpec spec = new PBEKeySpec(value.toCharArray(), SALT.getBytes(), 65536, 128);
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

    byte[] hash = factory.generateSecret(spec).getEncoded();
    return new String(hash);
  }
}
