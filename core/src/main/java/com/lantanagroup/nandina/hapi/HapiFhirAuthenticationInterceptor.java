package com.lantanagroup.nandina.hapi;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import com.lantanagroup.nandina.auth.OAuth2Helper;
import com.lantanagroup.nandina.config.IAuthConfig;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;

public class HapiFhirAuthenticationInterceptor implements IClientInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(HapiFhirAuthenticationInterceptor.class);
  private String authHeader;

  public HapiFhirAuthenticationInterceptor(IAuthConfig authConfig) {

    switch (authConfig.getAuthMode()) {
      case Basic:
        String username = authConfig.getUsername();
        String password = authConfig.getPassword();
        logger.debug("Using basic credentials for FHIR authentication with username " + username);
        String credentials = username + ":" + password;
        String encoded = Base64.encodeBase64String(credentials.getBytes());
        this.authHeader = "Basic " + encoded;
        break;
      case Token:
        String preDefinedToken = authConfig.getToken();
        logger.debug("Using pre-defined token for FHIR authentication: " + preDefinedToken);
        this.authHeader = "Bearer " + preDefinedToken;
        break;
      case OAuth2:
        logger.debug("Using OAuth2 to retrieve a system token for FHIR authentication");
        String token = OAuth2Helper.getToken(
                authConfig.getTokenUrl(),
                authConfig.getUsername(),
                authConfig.getPassword(),
                authConfig.getScopes());

        if (!StringUtils.isEmpty(token)) {
          this.authHeader = "Bearer " + token;
          logger.debug("Retrieved system token for FHIR authentication: " + token);
        } else {
          logger.error("No system token to use for FHIR authentication");
        }

        break;
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
