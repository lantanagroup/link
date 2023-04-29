package com.lantanagroup.link.query.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.db.BulkStatusService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.BulkStatus;
import com.lantanagroup.link.db.model.BulkStatusResult;
import com.lantanagroup.link.db.model.BulkStatuses;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.query.auth.HapiFhirAuthenticationInterceptor;
import com.lantanagroup.link.query.auth.ICustomAuth;
import lombok.Setter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

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

    OkHttpClient client = new OkHttpClient();

    URL url = new URL(tenantService.getConfig().getFhirQuery().getFhirServerBase() + tenantService.getConfig().getRelativeBulkUrl().replace("{groupId}", tenantService.getConfig().getBulkGroupId()));
    var requestBuilder = new Request.Builder()
            .url(url);

    setAuthHeaders(requestBuilder, tenantService, context);

    Request request = requestBuilder.build();
    Response response = client.newCall(request).execute();

    if(!response.isSuccessful()) {
      //figure out what to do here.
    }

    String pollingUrlresponse = response.header(tenantService.getConfig().getBulkInitiateResponseUrlHeader());
    bulkStatus.setStatusUrl(pollingUrlresponse);
    service.saveBulkStatus(bulkStatus);
  }
  public void executeStatusCheck(TenantService tenantService, BulkStatus bulkStatus) {

  }

  public void getStatus(BulkStatus bulkStatus,
                        TenantService tenantService,
                        BulkStatusService bulkStatusService,
                        ApplicationContext context) throws Exception {
    OkHttpClient client = new OkHttpClient();
    URL url = new URL(bulkStatus.getStatusUrl());
    var requestBuilder = new Request.Builder()
            .url(url);
    setAuthHeaders(requestBuilder, tenantService, context);
    Request request = requestBuilder.build();

    boolean progressComplete = false;
    String responseBody = null;

    while(!progressComplete){
      Response response = client.newCall(request).execute();

      if(!response.isSuccessful()) {
        //figure out what to do here.
      }

      var config = tenantService.getConfig();

      String progressHeader = response.header(config.getProgressHeaderName());

      if(progressHeader == null && progressHeader.trim().isEmpty()) {
        progressComplete = true;
        responseBody = response.body().string();
      }
    }

    BulkStatusResult statusResult = new BulkStatusResult();
    statusResult.setStatusId(bulkStatus.getId());
    statusResult.setResult(responseBody);

    bulkStatus.setStatus(BulkStatuses.Complete);
    bulkStatusService.saveBulkStatus(bulkStatus);
  }

  private void setAuthHeaders(Request.Builder requestBuilder, TenantService tenantService, ApplicationContext context) throws Exception {

    Class<?> authClass = Class.forName(tenantService.getConfig().getFhirQuery().getAuthClass());
    var authorizer = (ICustomAuth) context.getBean(authClass);
    authorizer.setTenantService(tenantService);

    String apiKey = authorizer.getApiKeyHeader();
    String authHeader = authorizer.getAuthHeader();

    if (authHeader != null && !authHeader.isEmpty()) {
      requestBuilder.addHeader("Authorization", authHeader);
    }
    if (apiKey != null && !apiKey.isEmpty()) {
      requestBuilder.addHeader("apikey", apiKey);
    }
  }
}
