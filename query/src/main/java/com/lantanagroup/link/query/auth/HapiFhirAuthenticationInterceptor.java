package com.lantanagroup.link.query.auth;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import com.lantanagroup.link.config.query.QueryConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

public class HapiFhirAuthenticationInterceptor implements IClientInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(HapiFhirAuthenticationInterceptor.class);
  private String authHeader;
  private String apiKey;

  public HapiFhirAuthenticationInterceptor(QueryConfig queryConfig, ApplicationContext context) throws ClassNotFoundException {
    if (StringUtils.isEmpty(queryConfig.getAuthClass())) {
      return;
    }

    // Get the Class definition of the auth class specified in config
    Class<?> authClass = Class.forName(queryConfig.getAuthClass());

    // Get an instance of the class using Spring so that it injects/autowires
    logger.debug(String.format("Getting an instance of the auth class \"%s\" from Spring", queryConfig.getAuthClass()));
    ICustomAuth authorizer = (ICustomAuth) context.getBean(authClass);

    try {
      logger.debug("Requesting Authorization header from auth class");
      this.authHeader = authorizer.getAuthHeader();
      this.apiKey = authorizer.getApiKeyHeader();
    } catch (Exception ex) {
      logger.error("Error establishing Authorization header of FHIR server request: " + ex.getMessage());
    }
  }

  @Override
  public void interceptRequest(IHttpRequest iHttpRequest) {
    if (this.authHeader != null && !this.authHeader.isEmpty()) {
      iHttpRequest.addHeader("Authorization", this.authHeader);
    }
    if (this.apiKey != null && !this.apiKey.isEmpty()) {
      iHttpRequest.addHeader("apikey", this.apiKey);
    }
  }

  @Override
  public void interceptResponse(IHttpResponse iHttpResponse) throws IOException {
    // nothing
  }
}
