package com.lantanagroup.link.auth;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.SigningKeyNotFoundException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.CernerClaimData;
import com.nimbusds.jose.jwk.ECKey;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

public class OAuth2Helper {
  private static final Logger logger = LoggerFactory.getLogger(OAuth2Helper.class);
  private static HashMap<String, String> issuerJwksUrls = new HashMap<>();
  private static final ApiConfig config = new ApiConfig();

  @VisibleForTesting
  static URL url;

  private static Integer connectTimeout;
  private static Integer readTimeout;
  private final static ObjectReader reader  = (new ObjectMapper()).readerFor(Map.class);

  public static String getPasswordCredentialsToken(HttpClient httpClient, String tokenUrl, String username, String password, String clientId, String scope) {
    HttpPost request = new HttpPost(tokenUrl);

    request.addHeader("Accept", "application/json");
    request.addHeader("Content-Type", "application/x-www-form-urlencoded");

    StringBuilder sb = new StringBuilder();
    sb.append("grant_type=password&");
    sb.append("scope=" + scope + "&");
    sb.append("username=" + username + "&");
    sb.append("password=" + password + "&");
    sb.append("client_id=" + clientId);

    try {
      request.setEntity(new StringEntity(sb.toString()));
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      return null;
    }

    try {
      if (httpClient == null) {
        httpClient = HttpClientBuilder.create().build();
      }

      HttpResponse result = httpClient.execute(request);

      String content = EntityUtils.toString(result.getEntity(), "UTF-8");

      if (result.getStatusLine() == null || result.getStatusLine().getStatusCode() != 200) {
        logger.error("Error retrieving OAuth2 password token from auth service: \n" + content);
      }

      JSONObject jsonObject = new JSONObject(content);

      if (jsonObject.has("access_token")) {
        return jsonObject.getString("access_token");
      }

      return null;
    } catch (IOException ex) {
      logger.error("Failed to retrieve a password token from OAuth2 authorization service", ex);
      return null;
    }
  }

  public static String getClientCredentialsToken(HttpClient httpClient, String tokenUrl, String username, String password, String scope) {
    HttpPost request = new HttpPost(tokenUrl);

    String userPassCombo = username + ":" + password;
    String authorization = Base64.getEncoder().encodeToString(userPassCombo.getBytes());

    request.addHeader("Accept", "application/json");
    request.addHeader("Content-Type", "application/x-www-form-urlencoded");
    request.addHeader("Authorization", "Basic " + authorization);
    request.addHeader("Cache-Control", "no-cache");

    StringBuilder sb = new StringBuilder();
    sb.append("grant_type=client_credentials&");
    sb.append("scope=" + scope);

    try {
      request.setEntity(new StringEntity(sb.toString()));
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      return null;
    }

    try {
      if (httpClient == null) {
        httpClient = HttpClientBuilder.create().build();
      }

      HttpResponse result = httpClient.execute(request);

      String content = EntityUtils.toString(result.getEntity(), "UTF-8");

      if (result.getStatusLine() == null || result.getStatusLine().getStatusCode() != 200) {
        logger.error("Error retrieving OAuth2 client credentials token from auth service: \n" + content);
      }

      JSONObject jsonObject = new JSONObject(content);

      if (jsonObject.has("access_token")) {
        return jsonObject.getString("access_token");
      }

      return null;
    } catch (IOException ex) {
      logger.error("Failed to retrieve a client credentials token from OAuth2 authorization service", ex);
      return null;
    }
  }

  private static Map<String, Object> getJwks() throws SigningKeyNotFoundException {
    try {
      URLConnection c = url.openConnection();
      if (connectTimeout != null) {
        c.setConnectTimeout(connectTimeout);
      }

      if (readTimeout != null) {
        c.setReadTimeout(readTimeout);
      }

      c.setRequestProperty("Accept", "application/json");
      InputStream inputStream = c.getInputStream();
      Throwable var3 = null;

      Map var4;
      try {
        var4 = (Map) reader.readValue(inputStream);
      } catch (Throwable var14) {
        var3 = var14;
        throw var14;
      } finally {
        if (inputStream != null) {
          if (var3 != null) {
            try {
              inputStream.close();
            } catch (Throwable var13) {
              var3.addSuppressed(var13);
            }
          } else {
            inputStream.close();
          }
        }

      }

      return var4;
    } catch (IOException var16) {
      throw new SigningKeyNotFoundException("Cannot obtain jwks from url " + url.toString(), var16);
    }
  }

  private static List<Jwk> getAll() throws SigningKeyNotFoundException {
    List<Jwk> jwks = Lists.newArrayList();
    List<Map<String, Object>> keys = (List) getJwks().get("keys");
    if (keys != null && !keys.isEmpty()) {
      try {
        Iterator var3 = keys.iterator();

        while (var3.hasNext()) {
          Map<String, Object> values = (Map) var3.next();
          jwks.add(Jwk.fromValues(values));
        }

        return jwks;
      } catch (IllegalArgumentException var5) {
        throw new SigningKeyNotFoundException("Failed to parse jwk from json", var5);
      }
    } else {
      throw new SigningKeyNotFoundException("No keys found in " + url.toString(), (Throwable) null);
    }
  }

  private static Jwk get(String keyId) throws JwkException {
    List<Jwk> jwks = getAll();
    if (keyId == null && jwks.size() == 1) {
      return (Jwk) jwks.get(0);
    } else {
      if (keyId != null) {
        Iterator var3 = jwks.iterator();

        while (var3.hasNext()) {
          Jwk jwk = (Jwk) var3.next();
          if (keyId.equals(jwk.getId())) {
            return jwk;
          }
        }
      }

      throw new SigningKeyNotFoundException("No key found in " + url.toString() + " with kid " + keyId, (Throwable) null);
    }
  }

  public static String getClientCredentialsToken(String tokenUrl, String username, String password, String scope) {
    CloseableHttpClient httpClient = HttpClientBuilder.create().build();
    return getClientCredentialsToken(httpClient, tokenUrl, username, password, scope);
  }

  private static CernerClaimData getCernerClaimData (DecodedJWT jwt) {
    Claim cernerClaim = jwt.getClaim("urn:cerner:authorization:claims:version:1");

    if (cernerClaim != null) {
      Map<String, Object> data = cernerClaim.asMap();
      CernerClaimData ccd = new CernerClaimData();

      if (data.containsKey("tenant")) {
        ccd.setTenant((String) data.get("tenant"));
      }

      return ccd;
    }

    return null;
  }

  private static String getJwksUrl (String openIdConfigUrl) {
    try {
      URL url = new URL(openIdConfigUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      int status = conn.getResponseCode();

      BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String inputLine;
      StringBuffer content = new StringBuffer();
      while ((inputLine = in.readLine()) != null) {
        content.append(inputLine);
      }
      in.close();

      JSONObject openIdConfigObj = new JSONObject(content.toString());
      return openIdConfigObj.getString("jwks_uri");
    } catch (Exception ex) {
      logger.error("Attempted to test/query openid config URL and got an error in response: " + openIdConfigUrl);
      return null;
    }
  }

  private static String getJwksUrl (DecodedJWT jwt) {
    //TODO: Need to do issuer verification
    Claim issuerClaim = jwt.getClaim("iss");

    if (issuerClaim != null && !issuerClaim.isNull()) {
      String issuer = issuerClaim.asString();

      if (issuerJwksUrls.containsKey(issuer)) {
        return issuerJwksUrls.get(issuer);
      }

      String openIdConfigUrl = issuer + (issuer.endsWith("/") ? "" : "/") + ".well-known/openid-configuration";
      String url = getJwksUrl(openIdConfigUrl);

      if (url == null && (issuer.equals("https://authorization.sandboxcerner.com/") || issuer.equals("https://authorization.cerner.com/"))) {
        CernerClaimData ccd = getCernerClaimData(jwt);

        if (ccd != null && ccd.getTenant() != null && !ccd.getTenant().isEmpty()) {
          String cernerConfigUrl = String.format("%stenants/%s/oidc/idsps/%s/.well-known/openid-configuration", issuer, ccd.getTenant(), ccd.getTenant());
          url = getJwksUrl(cernerConfigUrl);
        }
      }

      if (url != null) {
        issuerJwksUrls.put(issuer, url);
        return url;
      }
    }

    return config.getAuthJwksUrl();
  }

  private static DecodedJWT getValidationJWT (String token) {
    DecodedJWT jwt = JWT.decode(token);
    Claim idTokenClaim = jwt.getClaim("id_token");

    if (idTokenClaim != null && !idTokenClaim.isNull()) {         // this is smart-on-fhir
      String idToken = idTokenClaim.asString();
      return JWT.decode(idToken);
    }

    return jwt;
  }

  public static Boolean validateAuthHeader(String authHeader){


    String token = authHeader.substring("Bearer ".length());
    DecodedJWT jwt = getValidationJWT(token);

    try {
      url = new URL(getJwksUrl(jwt));
      Jwk jwk = get(jwt.getKeyId());
      Algorithm algorithm = null;
      PublicKey publicKey = null;

      if (jwk.getType() == null || jwk.getType().isEmpty()) {
        throw new Exception("JWK algorithm cannot be null");
      }

      switch (jwk.getType()) {
        case "RSA":
        case "rsa":
          publicKey = jwk.getPublicKey();
          algorithm = Algorithm.RSA256((RSAPublicKey) publicKey, null);
          break;
        case "EC":
        case "ec":
          JSONObject jwkJsonObj = new JSONObject();
          jwkJsonObj.put("x", jwk.getAdditionalAttributes().get("x"));
          jwkJsonObj.put("y", jwk.getAdditionalAttributes().get("y"));
          jwkJsonObj.put("crv", jwk.getAdditionalAttributes().get("crv"));
          jwkJsonObj.put("kty", jwk.getType());
          publicKey = ECKey.parse(jwkJsonObj.toString()).toECPublicKey();
          algorithm = Algorithm.ECDSA256((ECPublicKey) publicKey, null);
          break;
        default:
          throw new Exception("Unsupported JWK algorithm " + jwk.getAlgorithm());
      }

      algorithm.verify(jwt);
      if (jwt.getExpiresAt().before(new Date())) {
        throw new TokenExpiredException("Token has expired.");
      }

      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;

    }
  }

}
