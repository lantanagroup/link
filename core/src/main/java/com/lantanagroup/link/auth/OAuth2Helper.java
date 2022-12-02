package com.lantanagroup.link.auth;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.SigningKeyNotFoundException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.config.auth.LinkOAuthConfig;
import com.lantanagroup.link.model.CernerClaimData;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.AuthenticationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.regex.Pattern;

public class OAuth2Helper {
  private static final Logger logger = LoggerFactory.getLogger(OAuth2Helper.class);
  private final static ObjectReader reader = (new ObjectMapper()).readerFor(Map.class);
  @VisibleForTesting
  static URL url;
  private static HashMap<String, String> issuerJwksUrls = new HashMap<>();
  private static Integer connectTimeout;
  private static Integer readTimeout;

  //token algorithms
  public static enum TokenAlgorithmsEnum {
    RSA256,
    HS256,
    EC
  }

  public static Boolean validateHeaderJwtToken(String jwtToken) {
    //validate token
    //https://www.regextester.com/105777
    String allowedHeaderCharacters = "^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*$";
    return Pattern.matches(allowedHeaderCharacters, jwtToken);
  }

  public static String getToken(LinkOAuthConfig config) throws Exception {

    //ensure authentication properties have been set
    if(!config.hasCredentialProperties()) {
      throw new AuthenticationException("Authentication credentials were not supplied.");
    }

    //TODO - Add request type to LinkOAuthConfig and use in switch, RequestType: Submitting, LoadingMeasureDef, QueryingEHR

    //get token based on credential mode
    switch(config.getCredentialMode().toLowerCase(Locale.ROOT)) {
      case "client": {
        return getClientCredentialsToken(config.getTokenUrl(), config.getClientId(), config.getPassword(), config.getScope());
      }
      case "password": {
        return getPasswordCredentialsToken(config.getTokenUrl(), config.getUsername(), config.getPassword(), config.getClientId(), config.getScope());
      }
      case "sams-password": {
        return getSamsPasswordCredentialsToken(config.getTokenUrl(), config.getUsername(), config.getPassword(), config.getClientId(), config.getClientSecret(), config.getScope());
      }

      default:
        throw new AuthenticationException("Invalid credential mode.");
    }

  }

  public static String getPasswordCredentialsToken(String tokenUrl, String username, String password, String clientId, String scope) {
    try(CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
      return getPasswordCredentialsToken(httpClient, tokenUrl, username, password, clientId, scope);
    }
    catch(IOException ex) {
      logger.error(ex.getMessage());
      return "";
    }
  }

  public static String getPasswordCredentialsToken(CloseableHttpClient httpClient, String tokenUrl, String username, String password, String clientId, String scope) {
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
        logger.error("Error retrieving OAuth2 password token from auth service: " + content);
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

  public static String getSamsPasswordCredentialsToken(String tokenUrl, String username, String password, String clientId, String clientSecret, String scope) {
    try(CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
      return getSamsPasswordCredentialsToken(httpClient, tokenUrl, username, password, clientId, clientSecret, scope);
    }
    catch(IOException ex) {
      logger.error(ex.getMessage());
      return "";
    }
  }

  public static String getSamsPasswordCredentialsToken(CloseableHttpClient httpClient, String tokenUrl, String username, String password, String clientId, String clientSecret, String scope) {
    HttpPost request = new HttpPost(tokenUrl);

    request.addHeader("Accept", "application/json");
    request.addHeader("Content-Type", "application/x-www-form-urlencoded");

    StringBuilder sb = new StringBuilder();
    sb.append("grant_type=password");
    sb.append("&scope=" + scope);
    sb.append("&username=" + username);
    sb.append("&password=" + password);
    sb.append("&client_id=" + clientId);
    sb.append("&client_secret" + clientSecret);

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
        logger.error("Error retrieving OAuth2 password token from auth service: " + content);
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

  public static String getClientCredentialsToken(String tokenUrl, String clientId, String clientSecret, String scope) {
    try(CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
      return getClientCredentialsToken(httpClient, tokenUrl, clientId, clientSecret, scope);
    }
    catch(IOException ex) {
      logger.error(ex.getMessage());
      return "";
    }
  }

  public static String getClientCredentialsToken(CloseableHttpClient httpClient, String tokenUrl, String clientId, String clientSecret, String scope) {
    HttpPost request = new HttpPost(tokenUrl);

    //String userPassCombo = username + ":" + password;
    //String authorization = Base64.getEncoder().encodeToString(userPassCombo.getBytes());

    request.addHeader("Accept", "application/json");
    request.addHeader("Content-Type", "application/x-www-form-urlencoded");
    //request.addHeader("Authorization", "Basic " + authorization);
    request.addHeader("Cache-Control", "no-cache");

    StringBuilder sb = new StringBuilder();
    sb.append("grant_type=client_credentials");
    sb.append("&client_id=" + clientId);
    sb.append("&client_secret=" + clientSecret);
    sb.append("&scope=" + scope);

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
        logger.error("Error retrieving OAuth2 client credentials token from auth service");
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

      Map<String, Object> var4;
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
        Iterator<Map<String, Object>> var3 = keys.iterator();

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
      return jwks.get(0);
    } else {
      if (keyId != null) {
        Iterator<Jwk> var3 = jwks.iterator();

        while (var3.hasNext()) {
          Jwk jwk = var3.next();
          if (keyId.equals(jwk.getId())) {
            return jwk;
          }
        }
      }

      throw new SigningKeyNotFoundException("No key found in " + url.toString() + " with kid " + keyId, (Throwable) null);
    }
  }

  private static CernerClaimData getCernerClaimData(DecodedJWT jwt) {
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

  private static String getJwksUrl(String openIdConfigUrl) {
    try {
      URL url = new URL(openIdConfigUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      int status = conn.getResponseCode();

      StringBuffer content = new StringBuffer();
      try(InputStreamReader streamReader = new InputStreamReader(conn.getInputStream())) {

        BufferedReader in = new BufferedReader(streamReader);
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
          content.append(inputLine);
        }
        in.close();
      }

      JSONObject openIdConfigObj = new JSONObject(content.toString());
      return openIdConfigObj.getString("jwks_uri");
    } catch (Exception ex) {
      logger.error("Attempted to test/query openid config URL and got an error in response: " + openIdConfigUrl);
      return null;
    }
  }

  private static DecodedJWT getValidationJWT(String token) {
    DecodedJWT jwt = JWT.decode(token);
    Claim idTokenClaim = jwt.getClaim("id_token");

    if (idTokenClaim != null && !idTokenClaim.isNull()) {         // this is smart-on-fhir
      String idToken = idTokenClaim.asString();
      return JWT.decode(idToken);
    }

    return jwt;
  }

  /**
  * Verifies a JSON Web Token by accepting the auth header along with the issuer and alogirithm if supplied.
   * @param authHeader supplied auth header from the request
   * @param algo The algorithm used to create the token key, can be null if you want to determine through the JWK from the oauth endpoint
   * @param issuer The issuer of the JWT
   * @param jwksUrl The url of the JWKS store
   * @return if verified, return the decoded JWT
   *
  * */
  public static DecodedJWT verifyToken(String authHeader, TokenAlgorithmsEnum algo, String issuer, String jwksUrl) {
    String token = authHeader.substring("Bearer ".length());

    try {
      //decode received token to verify against jwks, this should also validate a correctly formatted token was received
      DecodedJWT jwt = getValidationJWT(token);

      if(jwksUrl == null || jwksUrl.isEmpty()) {
        throw new Exception("No URL was supplied to determine JWKS.");
      }

      //retrieve and validate web key store
      url = new URL(jwksUrl);
      List<Jwk> jwks = getAll();
      if(jwks == null || jwks.isEmpty()) {
        throw new JWTVerificationException("Failed to acquire public keys");
      }

      //check to ensure the key id from the jwt matches the key id from the issuer
      String issuerKid = "";
      String jwtKid = jwt.getKeyId();
      for(int i = 0; i < jwks.size(); i++) {
        if(jwks.get(i).getId().equals(jwtKid)) {
          issuerKid = jwtKid;
          break;
        }
      }

      //if no matching key id was found, throw a JWT verification exception
      if(issuerKid == null || issuerKid.isEmpty()) {
        throw new JWTVerificationException("Invalid key id");
      }

      //get jwk using verified key id
      Jwk jwk = get(issuerKid);

      Algorithm algorithm;
      //check if algorithm is supplied, if not use jwk to determine algorithm used
      if(algo != null) {
        switch(algo) {
          case RSA256: {
            algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null); // only the public key is used during verification
            break;
          }
          default:
            throw new Exception("Unsupported JWK algorithm");

        }
      }
      else {

        switch(jwk.getAlgorithm()) {
          case "RS256": {
            algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null); // only the public key is used during verification
            break;
          }
//          case "HS256": {
//            //algorithm = Algorithm.HMAC256()
//            break;
//          }
          default:
            throw new Exception("Unsupported JWK algorithm");

        }
      }

      //verify token
      JWTVerifier verifier = JWT.require(algorithm)
              .withIssuer(issuer)
              .acceptLeeway(10000)
              .build(); //Reusable verifier instance
      DecodedJWT verifiedJwt = verifier.verify(token);

      return verifiedJwt;

    } catch(JWTVerificationException e){
      //Invalid signature/claims
      throw new JWTVerificationException(e.getMessage());
    } catch(Exception e) {
      logger.error(e.getMessage());
      return null;
      //throw new Exception(e.getMessage());
    }

  }

  public static String[] getUserRoles(DecodedJWT jwt) {
    String[] noRoles = {};
    Map<String, Claim> claims = jwt.getClaims();
    Claim claim = claims.get("realm_access");
    if (claim != null) {
      Map<String, Object> backMap = claim.asMap();
      List<String> roles = (ArrayList) backMap.get(Constants.Roles);
      return roles.toArray(new String[roles.size()]);
    }
    return noRoles;
  }

  public static String getToken(LinkOAuthConfig authConfig, CloseableHttpClient client) throws Exception {
    String token = "";

    if (authConfig != null && authConfig.hasCredentialProperties()) {
      logger.info("Configured to authentication when submitting. Requesting a token from configured token URL");

      switch (authConfig.getCredentialMode().toLowerCase(Locale.ROOT)) {
        case "client":
          token = OAuth2Helper.getClientCredentialsToken(
                  client,
                  authConfig.getTokenUrl(),
                  authConfig.getClientId(),
                  authConfig.getPassword(),
                  authConfig.getScope());
          break;
        case "password":
          token = OAuth2Helper.getPasswordCredentialsToken(
                  client,
                  authConfig.getTokenUrl(),
                  authConfig.getUsername(),
                  authConfig.getPassword(),
                  authConfig.getClientId(),
                  authConfig.getScope());
          break;
        case "sams-password":
          token = OAuth2Helper.getSamsPasswordCredentialsToken(
                  client,
                  authConfig.getTokenUrl(),
                  authConfig.getUsername(),
                  authConfig.getPassword(),
                  authConfig.getClientId(),
                  authConfig.getClientSecret(),
                  authConfig.getScope());
          break;
      }
    } else {
      throw new Exception("Authentication is required to submit");
    }

    return token;
  }
}
