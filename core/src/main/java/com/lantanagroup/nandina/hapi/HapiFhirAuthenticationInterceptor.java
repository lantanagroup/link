package com.lantanagroup.nandina.hapi;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import com.lantanagroup.nandina.NandinaConfig;
import com.lantanagroup.nandina.auth.OAuth2Helper;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class HapiFhirAuthenticationInterceptor implements IClientInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(HapiFhirAuthenticationInterceptor.class);
  private String authHeader;

  public HapiFhirAuthenticationInterceptor(String token, NandinaConfig nandinaConfig) throws UnsupportedEncodingException {
    if (nandinaConfig.getFhirServerQueryAuth() != null) {
      switch (nandinaConfig.getFhirServerQueryAuth().get("mode")) {
        case "user-bearer-token":
          if (token != null) {
            this.authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
            logger.debug("Using user-bearer-token for FHIR authentication with token " + token);
          } else {
            logger.debug("Expected user-bearer-token but didn't find any");
          }
          break;
        case "basic":
          String username = nandinaConfig.getFhirServerQueryAuth().get("username");
          String password = nandinaConfig.getFhirServerQueryAuth().get("password");
          logger.debug("Using basic credentials for FHIR authentication with username " + username);
          String credentials = username + ":" + password;
          String encoded = Base64.encodeBase64String(credentials.getBytes());
          this.authHeader = "Basic " + encoded;
          break;
        case "token":
          String preDefinedToken = nandinaConfig.getFhirServerQueryAuth().get("token");
          logger.debug("Using pre-defined token for FHIR authentication: " + preDefinedToken);
          this.authHeader = "Bearer " + preDefinedToken;
          break;
        case "oauth2":
          logger.debug("Using OAuth2 to retrieve a system token for FHIR authentication");
          String tokenUrl = nandinaConfig.getFhirServerQueryAuth().get("tokenUrl");
          String tokenUsername = nandinaConfig.getFhirServerQueryAuth().get("username");
          String tokenPassword = nandinaConfig.getFhirServerQueryAuth().get("password");
          String scopes = nandinaConfig.getFhirServerQueryAuth().get("scopes");
          token = OAuth2Helper.getToken(tokenUrl, tokenUsername, tokenPassword, scopes);

          if (!StringUtils.isEmpty(token)) {
            this.authHeader = "Bearer " + token;
            logger.debug("Retrieved system token for FHIR authentication: " + token);
          } else {
            logger.error("No system token to use for FHIR authentication");
          }

          break;
      }
    }
  }

  @Override
  public void interceptRequest(IHttpRequest iHttpRequest) {
    if (this.authHeader != null && !this.authHeader.isEmpty()) {
      iHttpRequest.addHeader("Authorization", this.authHeader);
    }
  }

  @Override
  public void interceptResponse(IHttpResponse iHttpResponse) throws IOException {
    // nothing
  }
}
