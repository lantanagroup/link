package com.lantanagroup.link.query.auth;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.db.TenantService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Setter;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
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

  @Setter
  private TenantService tenantService;

  @Setter
  private HttpClient client = HttpClient.newHttpClient();

  public static String getJwt(com.lantanagroup.link.db.model.tenant.auth.EpicAuth config) {
    Key key = null;

    try {
      byte[] decodedKey = Base64.decodeBase64(config.getKey());
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      key = kf.generatePrivate(keySpec);
    } catch (Exception ex) {
      logger.error("Error loading private key from config for Epic auth: " + ex.getMessage());
    }

    logger.debug("Generating JWT for client id " + config.getClientId() + " and key that starts with " + config.getKey().substring(0, 10) + "...");

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
  public String getAuthHeader() throws URISyntaxException {
    if (this.tenantService.getConfig().getFhirQuery() == null) {
      logger.error("Tenant {} not configured to query FHIR", this.tenantService.getConfig().getId());
      return null;
    }

    if (this.tenantService.getConfig().getFhirQuery().getEpicAuth() == null) {
      logger.error("Tenant {} not configured for Epic authentication", this.tenantService.getConfig().getId());
      return null;
    }

    logger.debug("Generating JWT to request auth token from Epic");

    String jwt = getJwt(this.tenantService.getConfig().getFhirQuery().getEpicAuth());
    String requestBody = String.format("grant_type=client_credentials&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer&client_assertion=%s", jwt);

    logger.debug("Requesting token from " + this.tenantService.getConfig().getFhirQuery().getEpicAuth().getTokenUrl() + " with JWT:\n" + jwt);

    HttpRequest request = HttpRequest.newBuilder(new URI(this.tenantService.getConfig().getFhirQuery().getEpicAuth().getTokenUrl()))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

    try {
      HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
      String responseBody = response.body();
      Object responseObj = new Gson().fromJson(responseBody, Object.class);

      if (responseObj != null) {
        LinkedTreeMap<String, Object> responseTreeMap = (LinkedTreeMap<String, Object>) responseObj;

        if (responseTreeMap.containsKey("access_token")) {
          String accessToken = (String) responseTreeMap.get("access_token");
          logger.debug("Acquired access token for Epic");
          return "Bearer " + accessToken;
        } else {
          logger.error("Response from auth token request does not include an 'access_token' property:\n" + Helper.sanitizeString(responseBody));
        }
      }
    } catch (Exception ex) {
      logger.error("Error retrieving authentication token from Epic: " + ex.getMessage(), ex);
    }

    return null;
  }

  @Override
  public String getApiKeyHeader() throws Exception {
    return null;
  }
}
