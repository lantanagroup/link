package com.lantanagroup.link;

import com.password4j.Password;
import com.password4j.SaltGenerator;

/**
 * Used for creating a hash of (for example) passwords.
 */
public class Hasher {
  public static byte[] getRandomSalt() {
    return SaltGenerator.generate();
  }

  public static String hash(String value, byte[] salt) {
    return Password.hash(value)
            .addSalt(salt)
            .withArgon2()
            .getResult();
  }

  public static boolean check(String value, byte[] salt, String hash) {
    return Password.check(value, hash)
            .addSalt(salt)
            .withArgon2();
  }
}
