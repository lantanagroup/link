package com.lantanagroup.nandina.hapi;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import com.lantanagroup.nandina.Config;
import com.lantanagroup.nandina.Helper;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;

public class HapiFhirAuthenticationInterceptor implements IClientInterceptor {
  private String authHeader;

  public HapiFhirAuthenticationInterceptor(String token) {
    if (token != null) {
      this.authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
    } else if (!Helper.isNullOrEmpty(Config.getInstance().getFhirServerBearerToken())) {
      this.authHeader = "Bearer " + Config.getInstance().getFhirServerBearerToken();
    } else if (!Helper.isNullOrEmpty(Config.getInstance().getFhirServerUserName()) && !Helper.isNullOrEmpty(Config.getInstance().getFhirServerPassword())) {
      String credentials = Config.getInstance().getFhirServerUserName() + ":" + Config.getInstance().getFhirServerPassword();
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
