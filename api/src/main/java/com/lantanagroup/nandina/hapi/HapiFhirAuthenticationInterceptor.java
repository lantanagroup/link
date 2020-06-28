package com.lantanagroup.nandina.hapi;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.JsonProperties;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;

public class HapiFhirAuthenticationInterceptor implements IClientInterceptor {
  private String authHeader;

  public HapiFhirAuthenticationInterceptor(String token, JsonProperties jsonProperties) {
    if (token != null) {
      this.authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
    } else if (!Helper.isNullOrEmpty(jsonProperties.getFhirServerBearerToken())) {
      this.authHeader = "Bearer " + jsonProperties.getFhirServerBearerToken();
    } else if (!Helper.isNullOrEmpty(jsonProperties.getFhirServerUserName()) && !Helper.isNullOrEmpty(jsonProperties.getFhirServerPassword())) {
      String credentials = jsonProperties.getFhirServerUserName() + ":" + jsonProperties.getFhirServerPassword();
      String encoded = Base64.encodeBase64String(credentials.getBytes());
      this.authHeader = "Basic " + encoded;
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
