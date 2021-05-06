package com.lantanagroup.link.query.auth;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.UUID;

public class EpicAuth implements ICustomAuth {
  private static final Logger logger = LoggerFactory.getLogger(EpicAuth.class);

  @Autowired
  private EpicAuthConfig config;

  public EpicAuth() {

  }

  public EpicAuth(EpicAuthConfig config) {
    this.config = config;
  }

  public static String getJwt(EpicAuthConfig config) {
    Key key = null;

    try {
      byte[] decodedKey = Base64.decodeBase64(config.getKey());
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      key = kf.generatePrivate(keySpec);
    } catch (Exception ex) {
      logger.error("Error loading private key from config for Epic auth: " + ex.getMessage());
    }

    Date exp = new Date(System.currentTimeMillis() + (1000 * 60 * 4));  // extend 4 minutes
    String jwt = Jwts.builder()
            .setHeaderParam("typ", "JWT")
            .setIssuer(config.getClientId())
            .setSubject(config.getClientId())
            .setAudience(config.getAudience())
            .claim("jti", UUID.randomUUID().toString())
            .setExpiration(exp)
            .signWith(SignatureAlgorithm.RS256, key)
            .compact();
    return jwt;
  }

  @Override
  public String getAuthHeader() throws Exception {
    String jwt = getJwt(this.config);
    String requestBody = String.format("grant_type=client_credentials&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer&client_assertion=%s", jwt);

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder(new URI(this.config.getTokenUrl()))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

    HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
    String responseBody = (String) response.body();
    Object responseObj = new Gson().fromJson(responseBody, Object.class);

    if (responseObj != null) {
      LinkedTreeMap<String, Object> responseTreeMap = (LinkedTreeMap<String, Object>) responseObj;

      if (responseTreeMap.containsKey("access_token")) {
        return "Bearer " + (String) responseTreeMap.get("access_token");
      }
    }

    return null;
  }
}
