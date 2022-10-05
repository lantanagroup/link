package com.lantanagroup.link.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Base64;

@Getter @Setter @AllArgsConstructor
public class BasicAuthModel {
  private String username;
  private String password;

  public static BasicAuthModel getBasicAuth(String authHeader) {
    String base64Encoded = authHeader.substring(6);

    try {
      String base64Decoded = new String(Base64.getDecoder().decode(base64Encoded));
      String[] split = base64Decoded.split(":");

      if (split.length == 2) {
        return new BasicAuthModel(split[0], split[1]);
      }
    } catch (IllegalArgumentException ex) {
      return null;
    }

    return null;
  }
}
