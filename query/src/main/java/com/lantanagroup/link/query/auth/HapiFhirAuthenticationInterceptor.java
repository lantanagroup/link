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
  private volatile String clientSecret;
  private volatile String clientId;

  public HapiFhirAuthenticationInterceptor(TenantService tenantService, ApplicationContext context) throws ReflectiveOperationException {
    if (tenantService.getConfig().getFhirQuery() == null || StringUtils.isEmpty(tenantService.getConfig().getFhirQuery().getAuthClass())) {
      authorizer = null;
      return;
    }

    // Get the auth class specified in config
    Class<?> authClass = Class.forName(tenantService.getConfig().getFhirQuery().getAuthClass());
    authorizer = (ICustomAuth) authClass.getConstructor().newInstance();

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
      this.clientSecret = authorizer.getClientSecretHeader();
      this.clientId = authorizer.getClientIdHeader();
    } catch (Exception ex) {
      logger.error("Failed to refresh credentials", ex);
    }
  }

  private synchronized void refresh(IHttpRequest request) {
    if(this.authorizer instanceof CernerHeaderOnlyAuth) {
      String requestedClientSecretHeader = getHeader(request, "Client_Secret");
      String requestedClientIdHeader = getHeader(request, "client_id");
      if(!StringUtils.equals(requestedClientSecretHeader, this.clientSecret) ||
              !StringUtils.equals(requestedClientIdHeader, this.clientId)){
        // Another thread has already refreshed the credentials since we made this request
        return;
      }
    }
    else {
      String requestedAuthHeader = getHeader(request, "Authorization");
      String requestedApiKey = getHeader(request, "apikey");
      if (!StringUtils.equals(requestedAuthHeader, this.authHeader) || !StringUtils.equals(requestedApiKey, this.apiKey)) {
        // Another thread has already refreshed the credentials since we made this request
        return;
      }
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
    request.removeHeaders("client_id");
    request.removeHeaders("Client_Secret");
  }

  private void addHeaders(IHttpRequest request) {
    if (this.authHeader != null && !this.authHeader.isEmpty()) {
      request.addHeader("Authorization", this.authHeader);
    }
    if (this.apiKey != null && !this.apiKey.isEmpty()) {
      request.addHeader("apikey", this.apiKey);
    }
    if(this.clientSecret != null && !this.clientSecret.isEmpty()){
      request.addHeader("Client_Secret", this.clientSecret);
    }
    if(this.clientId != null && !this.clientId.isEmpty()){
      request.addHeader("client_id", this.clientId);
    }
    //}
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
