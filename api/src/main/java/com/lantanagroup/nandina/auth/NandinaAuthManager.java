package com.lantanagroup.nandina.auth;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lantanagroup.nandina.NandinaConfig;
import com.lantanagroup.nandina.model.CernerClaimData;
import com.nimbusds.jose.jwk.ECKey;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

public class NandinaAuthManager implements AuthenticationManager {
  private static final Logger logger = LoggerFactory.getLogger(NandinaAuthManager.class);
  private HashMap<String, String> issuerJwksUrls = new HashMap<>();
  private NandinaConfig nandinaConfig;

  public NandinaAuthManager(NandinaConfig nandinaConfig) {
    this.nandinaConfig = nandinaConfig;
  }

  private String getJwksUrl(String openIdConfigUrl) {
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

  private CernerClaimData getCernerClaimData(DecodedJWT jwt) {
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

  private String getJwksUrl(DecodedJWT jwt) {
    Claim issuerClaim = jwt.getClaim("iss");
    Claim tenantClaim = jwt.getClaim("tenant");

    if (issuerClaim != null && !issuerClaim.isNull()) {
      String issuer = issuerClaim.asString();

      if (this.issuerJwksUrls.containsKey(issuer)) {
        return this.issuerJwksUrls.get(issuer);
      }

      String openIdConfigUrl = issuer + (issuer.endsWith("/") ? "" : "/") + ".well-known/openid-configuration";
      String url = this.getJwksUrl(openIdConfigUrl);

      if (url == null && (issuer.equals("https://authorization.sandboxcerner.com/") || issuer.equals("https://authorization.cerner.com/"))) {
        CernerClaimData ccd = this.getCernerClaimData(jwt);

        if (ccd != null && ccd.getTenant() != null && !ccd.getTenant().isEmpty()) {
          String cernerConfigUrl = String.format("%stenants/%s/oidc/idsps/%s/.well-known/openid-configuration", issuer, ccd.getTenant(), ccd.getTenant());
          url = this.getJwksUrl(cernerConfigUrl);
        }
      }

      if (url != null) {
        this.issuerJwksUrls.put(issuer, url);
        return url;
      }
    }

    return nandinaConfig.getAuthJwksUrl();
  }

  private DecodedJWT getValidationJWT(String token) {
    DecodedJWT jwt = JWT.decode(token);
    Claim idTokenClaim = jwt.getClaim("id_token");

    if (idTokenClaim != null && !idTokenClaim.isNull()) {         // this is smart-on-fhir
      String idToken = idTokenClaim.asString();
      return JWT.decode(idToken);
    }

    return jwt;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String authHeader = (String) authentication.getPrincipal();

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new BadCredentialsException("This REST operation requires a Bearer Authorization header.");
    }

    // Validate that the token is issued by our configured authentication provider
    String token = authHeader.substring("Bearer ".length());
    DecodedJWT jwt = this.getValidationJWT(token);
    String jwksUrl = this.getJwksUrl(jwt);
    JwkProvider provider = new CustomUrlJwkProvider(jwksUrl);

    try {
      Jwk jwk = provider.get(jwt.getKeyId());
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

      authentication.setAuthenticated(true);
    } catch (Exception e) {
      authentication.setAuthenticated(false);
      e.printStackTrace();
    }

    return authentication;
  }
}
