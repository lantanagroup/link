package com.lantanagroup.link.query.auth;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import com.lantanagroup.link.config.query.QueryConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

@Interceptor
public class HapiFhirAuthenticationInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(HapiFhirAuthenticationInterceptor.class);
  private final ICustomAuth authorizer;
  private String authHeader;
  private String apiKey;

  public HapiFhirAuthenticationInterceptor(QueryConfig queryConfig, ApplicationContext context) throws ClassNotFoundException {
    if (StringUtils.isEmpty(queryConfig.getAuthClass())) {
      authorizer = null;
      return;
    }

    // Get the Class definition of the auth class specified in config
    Class<?> authClass = Class.forName(queryConfig.getAuthClass());

    // Get an instance of the class using Spring so that it injects/autowires
    logger.debug(String.format("Getting an instance of the auth class \"%s\" from Spring", queryConfig.getAuthClass()));
    authorizer = (ICustomAuth) context.getBean(authClass);
    refresh();
  }

  private void refresh() {
    if (authorizer == null) {
      return;
    }
    try {
      logger.debug("Requesting Authorization header from auth class");
      this.authHeader = authorizer.getAuthHeader();
      this.apiKey = authorizer.getApiKeyHeader();
    } catch (Exception ex) {
      logger.error("Error establishing Authorization header of FHIR server request: " + ex.getMessage());
    }
  }

  private void removeHeaders(IHttpRequest request) {
    request.removeHeaders("Authorization");
    request.removeHeaders("apikey");
  }

  private void addHeaders(IHttpRequest request) {
    if (this.authHeader != null && !this.authHeader.isEmpty()) {
      request.addHeader("Authorization", this.authHeader);
    }
    if (this.apiKey != null && !this.apiKey.isEmpty()) {
      request.addHeader("apikey", this.apiKey);
    }
  }

  private void copyResponse(HttpResponse source, HttpResponse target) {
    target.setStatusLine(source.getStatusLine());
    target.setEntity(source.getEntity());
    target.setLocale(source.getLocale());
  }

  @Hook(Pointcut.CLIENT_REQUEST)
  public void interceptRequest(IHttpRequest request) {
    addHeaders(request);
  }

  @Hook(Pointcut.CLIENT_RESPONSE)
  public void interceptResponse(IHttpRequest request, IHttpResponse response) throws IOException {
    if (response.getStatus() != 401) {
      return;
    }
    if (!(response.getResponse() instanceof HttpResponse)) {
      return;
    }
    refresh();
    removeHeaders(request);
    addHeaders(request);
    IHttpResponse newResponse = request.execute();
    if (!(newResponse.getResponse() instanceof HttpResponse)) {
      return;
    }
    copyResponse((HttpResponse) newResponse.getResponse(), (HttpResponse) response.getResponse());
  }
}
