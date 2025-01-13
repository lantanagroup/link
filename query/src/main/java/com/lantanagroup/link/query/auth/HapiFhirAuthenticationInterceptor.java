package com.lantanagroup.link.query.auth;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.client.api.ClientResponseContext;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import com.lantanagroup.link.db.TenantService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("unused")
@Interceptor
public class HapiFhirAuthenticationInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(HapiFhirAuthenticationInterceptor.class);
  private final ICustomAuth authorizer;
  private volatile String authHeader;
  private volatile String apiKey;

  public HapiFhirAuthenticationInterceptor(TenantService tenantService, ApplicationContext context) throws ClassNotFoundException {
    if (tenantService.getConfig().getFhirQuery() == null || StringUtils.isEmpty(tenantService.getConfig().getFhirQuery().getAuthClass())) {
      authorizer = null;
      return;
    }

    // Get the Class definition of the auth class specified in config
    Class<?> authClass = Class.forName(tenantService.getConfig().getFhirQuery().getAuthClass());

    // Get an instance of the class using Spring so that it injects/autowires
    logger.debug(String.format("Getting an instance of the auth class \"%s\" from Spring", tenantService.getConfig().getFhirQuery().getAuthClass()));
    authorizer = (ICustomAuth) context.getBean(authClass);

    authorizer.setTenantService(tenantService);

    refresh();
  }

  private void refresh() {
    if (authorizer == null) {
      return;
    }
    try {
      logger.debug("Refreshing credentials");
      this.authHeader = authorizer.getAuthHeader();
      this.apiKey = authorizer.getApiKeyHeader();
    } catch (Exception ex) {
      logger.error("Failed to refresh credentials", ex);
    }
  }

  private synchronized void refresh(IHttpRequest request) {
    String requestedAuthHeader = getHeader(request, "Authorization");
    String requestedApiKey = getHeader(request, "apikey");
    if (!StringUtils.equals(requestedAuthHeader, authHeader) || !StringUtils.equals(requestedApiKey, apiKey)) {
      // Another thread has already refreshed the credentials since we made this request
      return;
    }
    refresh();
  }

  private String getHeader(IHttpRequest request, String name) {
    List<String> headers = request.getAllHeaders().get(name);
    return CollectionUtils.isEmpty(headers) ? null : headers.get(0);
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

  @Hook(Pointcut.CLIENT_REQUEST)
  public void interceptRequest(IHttpRequest request) {
    addHeaders(request);
  }

  @Hook(Pointcut.CLIENT_RESPONSE)
  public void interceptResponse(IHttpRequest request, IHttpResponse response, ClientResponseContext context) throws IOException {
    if (response.getStatus() != 401) {
      return;
    }
    refresh(request);
    removeHeaders(request);
    addHeaders(request);
    context.setHttpResponse(request.execute());
    response.close();
  }
}
