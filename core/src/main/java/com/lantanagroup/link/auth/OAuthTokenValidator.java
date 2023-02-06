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
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.interfaces.RSAPublicKey;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class OAuthTokenValidator implements ITokenValidator {

  static URL url;
  private static final Logger logger = LoggerFactory.getLogger(OAuthTokenValidator.class);
  private final static ObjectReader reader = (new ObjectMapper()).readerFor(Map.class);

  @Override
  public boolean verifyToken(String authHeader, String algo, String issuer, String jwksUrl, String validationEndpoint) {

    ///TODO: Determine if there is a generic way that identity providers implementing ouath set up validation endpoints, if so check if not null and use the expected response for verification.

    //get token from auth header
    String token = authHeader.substring("Bearer ".length());

    try {

      //verify auth config values
      if(StringUtils.isEmpty(algo) || StringUtils.isEmpty(issuer) || StringUtils.isEmpty(jwksUrl)) {
        throw new Exception("Incomplete authentication configuration, cannot verify token.");
      }

      //decode received token to verify against jwks, this should also validate a correctly formatted token was received
      DecodedJWT jwt = getJWT(token);

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
      if(issuerKid.isEmpty()) {
        throw new JWTVerificationException("Invalid key id");
      }

      //get jwk using verified key id
      Jwk jwk = getJwk(issuerKid);

      Algorithm algorithm;
      //use configured algorithm to verify token
      switch(algo) {
        case "RSA256": {
          algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null); // only the public key is used during verification
          break;
        }
//          case "HS256": {
//            algorithm = Algorithm.HMAC256()
//            break;
//          }
        default:
          throw new Exception("Unsupported JWK algorithm");

      }

      //verify token
      JWTVerifier verifier = JWT.require(algorithm)
              .withIssuer(issuer)
              .acceptLeeway(10000)
              .build(); //Reusable verifier instance
      DecodedJWT verifiedJwt = verifier.verify(token);

      return verifiedJwt != null;

    } catch(JWTVerificationException e){
      //Invalid signature/claims
      throw new JWTVerificationException(e.getMessage());
    } catch(Exception e) {
      logger.error(e.getMessage());
      return false;
    }

  }

  private static DecodedJWT getJWT(String token) {
    DecodedJWT jwt = JWT.decode(token);
    Claim idTokenClaim = jwt.getClaim("id_token");

    if (idTokenClaim != null && !idTokenClaim.isNull()) {  // this is smart-on-fhir
      String idToken = idTokenClaim.asString();
      return JWT.decode(idToken);
    }

    return jwt;
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

  private static Map<String, Object> getJwks() throws SigningKeyNotFoundException {
    Integer connectTimeout = 5000;
    Integer readTimeout = 5000;

    try {
      URLConnection c = url.openConnection();
      c.setConnectTimeout(connectTimeout);
      c.setReadTimeout(readTimeout);
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

  private static Jwk getJwk(String keyId) throws JwkException {
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

}
