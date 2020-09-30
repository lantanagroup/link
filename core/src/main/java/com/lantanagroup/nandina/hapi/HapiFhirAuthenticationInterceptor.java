package com.lantanagroup.nandina.hapi;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import com.lantanagroup.nandina.NandinaConfig;
import com.lantanagroup.nandina.auth.OAuth2Helper;
import org.apache.commons.codec.binary.Base64;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class HapiFhirAuthenticationInterceptor implements IClientInterceptor {
  private String authHeader;

  public HapiFhirAuthenticationInterceptor(String token, NandinaConfig nandinaConfig) throws UnsupportedEncodingException {
    if (nandinaConfig.getFhirServerQueryAuth() != null) {
      switch (nandinaConfig.getFhirServerQueryAuth().get("mode")) {
        case "user-bearer-token":
          if (token != null) {
            this.authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
          }
          break;
        case "basic":
          String credentials = nandinaConfig.getFhirServerQueryAuth().get("username") + ":" + nandinaConfig.getFhirServerQueryAuth().get("password");
          String encoded = Base64.encodeBase64String(credentials.getBytes());
          this.authHeader = "Basic " + encoded;
          break;
        case "token":
          this.authHeader = "Bearer " + nandinaConfig.getFhirServerQueryAuth().get("token");
          break;
        case "oauth2":
          String tokenUrl = nandinaConfig.getFhirServerQueryAuth().get("tokenUrl");
          String username = nandinaConfig.getFhirServerQueryAuth().get("username");
          String password = nandinaConfig.getFhirServerQueryAuth().get("password");
          String scopes = nandinaConfig.getFhirServerQueryAuth().get("scopes");
          token = OAuth2Helper.getToken(tokenUrl, username, password, scopes);

          if (!StringUtils.isEmpty(token)) {
            this.authHeader = "Bearer " + token;
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
