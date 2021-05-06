package com.lantanagroup.link.query.auth;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import com.lantanagroup.link.config.QueryConfig;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

public class HapiFhirAuthenticationInterceptor implements IClientInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(HapiFhirAuthenticationInterceptor.class);
  private String authHeader;

  public HapiFhirAuthenticationInterceptor(QueryConfig queryConfig, ApplicationContext context) throws ClassNotFoundException {
    if (Strings.isEmpty(queryConfig.getAuthClass())) {
      return;
    }

    // Get the Class definition of the auth class specified in config
    Class authClass = Class.forName(queryConfig.getAuthClass());
    // Get an instance of the class using Spring so that it injects/autowires
    ICustomAuth authorizer = (ICustomAuth) context.getBean(authClass);

    try {
      this.authHeader = authorizer.getAuthHeader();
    } catch (Exception ex) {
      logger.error("Error establishing Authorization header of FHIR server request: " + ex.getMessage());
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
