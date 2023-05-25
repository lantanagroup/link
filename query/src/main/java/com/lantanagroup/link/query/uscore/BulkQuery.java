package com.lantanagroup.link.query.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import com.google.gson.Gson;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.db.BulkStatusService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.BulkStatus;
import com.lantanagroup.link.db.model.BulkStatusResult;
import com.lantanagroup.link.db.model.BulkStatuses;
import com.lantanagroup.link.model.BulkResponse;
import com.lantanagroup.link.query.auth.HapiFhirAuthenticationInterceptor;
import com.lantanagroup.link.query.auth.ICustomAuth;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class BulkQuery {
  private static final Logger logger = LoggerFactory.getLogger(BulkQuery.class);
  @Setter
  private IGenericClient fhirQueryClient;

  @Setter
  private ApplicationContext applicationContext;

  public IGenericClient getFhirQueryClient(TenantService tenantService) {
    if(this.fhirQueryClient != null) {
      return this.fhirQueryClient;
    }

    //this.getFhirContext().getRestfulClientFactory().setSocketTimeout(30 * 1000);   // 30 seconds
    IGenericClient fhirQueryClient = FhirContextProvider.getFhirContext()
            .newRestfulGenericClient(tenantService.getConfig().getFhirQuery().getFhirServerBase());

    LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
    loggingInterceptor.setLogRequestBody(true);
    fhirQueryClient.registerInterceptor(loggingInterceptor);

    if (StringUtils.isNotEmpty(tenantService.getConfig().getFhirQuery().getAuthClass())) {
      logger.debug(String.format("Authenticating queries using %s", tenantService.getConfig().getFhirQuery().getAuthClass()));

      try {
        fhirQueryClient.registerInterceptor(new HapiFhirAuthenticationInterceptor(tenantService, this.applicationContext));
      } catch (ClassNotFoundException e) {
        logger.error("Error registering authentication interceptor", e);
      }
    } else {
      logger.warn("No authentication is configured for the FHIR server being queried");
    }

    fhirQueryClient.forceConformanceCheck();

    this.fhirQueryClient = fhirQueryClient;
    return fhirQueryClient;
  }

  public void executeInitiateRequest(TenantService tenantService, BulkStatusService service, BulkStatus bulkStatus, ApplicationContext context) throws Exception {

    URI uri = new URI(tenantService.getConfig().getFhirQuery().getFhirServerBase() + tenantService.getConfig().getRelativeBulkUrl().replace("{groupId}", tenantService.getConfig().getBulkGroupId()));
    HttpClient httpClient = HttpClient.newHttpClient();
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri);
    setAuthHeaders(requestBuilder, tenantService, context);
    HttpRequest request = requestBuilder.build();
    HttpResponse<String> response = null;

    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      logger.warn("Error initiating bulk export", e);
      bulkStatus.setStatus(BulkStatuses.Pending);
      service.saveBulkStatus(bulkStatus);
    }

    assert response != null;
    if(response.statusCode() > 400) {
      logger.warn("Error initiating bulk export");
      bulkStatus.setStatus(BulkStatuses.Pending);
      service.saveBulkStatus(bulkStatus);
      return;
    }

    String pollingUrlresponse = response.headers().map().get(tenantService.getConfig().getBulkInitiateResponseUrlHeader()).get(0);
    bulkStatus.setStatusUrl(pollingUrlresponse);
    bulkStatus.setStatus(BulkStatuses.Pending);
    service.saveBulkStatus(bulkStatus);
  }
  public void executeStatusCheck(TenantService tenantService, BulkStatus bulkStatus) {

  }

  public void getStatus(BulkStatus bulkStatus,
                        TenantService tenantService,
                        BulkStatusService bulkStatusService,
                        ApplicationContext context) throws Exception {

    URI uri = new URI(bulkStatus.getStatusUrl());
    HttpClient httpClient = HttpClient.newHttpClient();
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri);
    setAuthHeaders(requestBuilder, tenantService, context);
    HttpRequest request = requestBuilder.build();

    boolean progressComplete = false;
    String responseBody = null;

    while(!progressComplete){
      //Response response = client.newCall(request).execute();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if(response.statusCode() > 400) {
        //figure out what to do here.
        throw new Exception("Fetch failed: " + uri.toString());
      }

      var config = tenantService.getConfig();
      String progressHeader = response.headers().map().get(config.getProgressHeaderName()).get(0);

      if(progressHeader == null && progressHeader.trim().isEmpty()) {
        progressComplete = true;
        responseBody = response.body();
      }
      Thread.sleep(config.getBulkWaitTimeInMilliseconds());
    }

    BulkStatusResult statusResult = new BulkStatusResult();
    statusResult.setStatusId(bulkStatus.getId());
    Gson gson = new Gson();
    BulkResponse bulkResponse = gson.fromJson(responseBody, BulkResponse.class);
    statusResult.setResult(bulkResponse);

    bulkStatus.setStatus(BulkStatuses.Complete);
    bulkStatusService.saveBulkStatus(bulkStatus);

    bulkStatusService.saveResult(statusResult);
  }

  private void setAuthHeaders(HttpRequest.Builder requestBuilder, TenantService tenantService, ApplicationContext context) throws Exception {
    Class<?> authClass = Class.forName(tenantService.getConfig().getFhirQuery().getAuthClass());
    var authorizer = (ICustomAuth) context.getBean(authClass);
    authorizer.setTenantService(tenantService);

    String apiKey = authorizer.getApiKeyHeader();
    String authHeader = authorizer.getAuthHeader();

    if (authHeader != null && !authHeader.isEmpty()) {
      requestBuilder.setHeader("Authorization", authHeader);
    }
    if (apiKey != null && !apiKey.isEmpty()) {
      requestBuilder.setHeader("apikey", apiKey);
    }
  }
}
