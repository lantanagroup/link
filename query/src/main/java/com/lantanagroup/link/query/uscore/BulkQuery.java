package com.lantanagroup.link.query.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import com.google.gson.Gson;
import com.lantanagroup.link.*;
import com.lantanagroup.link.db.BulkStatusService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.*;
import com.lantanagroup.link.db.model.tenant.QueryList;
import com.lantanagroup.link.model.BulkResponse;
import com.lantanagroup.link.query.auth.HapiFhirAuthenticationInterceptor;
import com.lantanagroup.link.query.auth.ICustomAuth;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Component
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

    var config = tenantService.getConfig();

    URI uri = new URI(tenantService.getConfig().getFhirQuery().getFhirServerBase() + tenantService.getConfig().getRelativeBulkUrl().replace("{groupId}", tenantService.getConfig().getBulkGroupId()));
    HttpClient httpClient = HttpClient.newHttpClient();
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri);
    requestBuilder.setHeader("Accept", "application/fhir+json");
    requestBuilder.setHeader("Prefer", "respond-async");
    setAuthHeaders(requestBuilder, tenantService, context);
    HttpRequest request = requestBuilder.build();
    HttpResponse<String> response = null;

    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      StringBuilder sbuilder = new StringBuilder();
      sbuilder.append("Error encountered running initiate bulk data request. Cancelling bulk status with id " + bulkStatus.getId() + " Exception: " + e.getMessage());
      sbuilder.append("Stack Trace:\n");
      sbuilder.append(e.getStackTrace());
      bulkStatus.setStatus(BulkStatuses.cancelled);
      bulkStatus.setErrorMessage(sbuilder.toString());
      service.saveBulkStatus(bulkStatus);
    } catch (InterruptedException e) {
      logger.warn("Interrupted while initiating bulk export", e);
      bulkStatus.setStatus(BulkStatuses.pending);
      service.saveBulkStatus(bulkStatus);
      Thread.currentThread().interrupt();
    }

    assert response != null;
    if(response.statusCode() > 400) {
      StringBuilder sbuilder = new StringBuilder();
      sbuilder.append("Error encountered running initiate bulk data request. Cancelling bulk status with id " + bulkStatus.getId() + " Status Code: " + response.statusCode());
      if(response.body().length() > 0){
        sbuilder.append("Response Body: " + Helper.sanitizeString(response.body()));
      }
      bulkStatus.setStatus(BulkStatuses.cancelled);
      service.saveBulkStatus(bulkStatus);
      logger.warn(sbuilder.toString());
      return;
    }

    var headers = response.headers();
    String facilityHeaderNameValue = tenantService.getConfig().getBulkInitiateResponseUrlHeader();
    var statusHeader = headers.firstValue(facilityHeaderNameValue);
    String pollingUrlresponse;
    try {
      pollingUrlresponse = statusHeader.get();
    } catch(Exception e){
      StringBuilder sbuilder = new StringBuilder();
      sbuilder.append("Error encountered running initiate bulk data request. Cancelling bulk status with id " + bulkStatus.getId() + " Exception: " + e.getMessage());
      sbuilder.append("Stack Trace:\n");
      sbuilder.append(e.getStackTrace());
      bulkStatus.setStatus(BulkStatuses.cancelled);
      bulkStatus.setErrorMessage(sbuilder.toString());
      service.saveBulkStatus(bulkStatus);
      logger.warn(sbuilder.toString());
      return;
//      throw new Exception(sbuilder.toString());
    }

    bulkStatus.setStatusUrl(pollingUrlresponse);
    bulkStatus.setStatus(BulkStatuses.pending);
    service.saveBulkStatus(bulkStatus);
  }
  public void executeStatusCheck(TenantService tenantService, BulkStatus bulkStatus) {

  }

  public BulkStatusResult getStatus(BulkStatus bulkStatus,
                        TenantService tenantService,
                        BulkStatusService bulkStatusService,
                        ApplicationContext context) throws Exception {

    boolean progressComplete = false;
    String responseBody = null;

    while(!progressComplete){

      URI uri = new URI(bulkStatus.getStatusUrl());
      HttpClient httpClient = HttpClient.newHttpClient();
      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri);
      setAuthHeaders(requestBuilder, tenantService, context);
      HttpRequest request = requestBuilder.build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if(response.statusCode() > 400) {
        //figure out what to do here.
        responseBody = response.body();
        StringBuilder sbuilder = new StringBuilder();
        sbuilder.append("Fetch failed for URI: " + uri.toString() + " Status Code: " + response.statusCode());
        if(responseBody.length() > 0){
          sbuilder.append("Response Body: " + Helper.sanitizeString(responseBody));
        }
        logger.warn(sbuilder.toString());
        return null;
        //throw new Exception(sbuilder.toString());
      }

      var config = tenantService.getConfig();
      List progressHeader = response.headers().map().get(config.getProgressHeaderName());

      if(progressHeader == null || progressHeader.size() == 0) {
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

    bulkStatus.setStatus(BulkStatuses.complete);
    bulkStatusService.saveBulkStatus(bulkStatus);

    bulkStatusService.saveBulkStatusResult(statusResult);

    return statusResult;
  }

  public void getResultSetFromBulkResultAndLoadPatientData(
          BulkStatusResult statusResult,
          TenantService tenantService,
          ApplicationContext context)
          throws Exception {


    Map<String,IBaseResource>  parsedData = new HashMap<>();

    for (var output : statusResult.getResult().getOutput()) {
      URI uri = new URI(output.getUrl());
      HttpClient httpClient = HttpClient.newHttpClient();
      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri);
      setAuthHeaders(requestBuilder, tenantService, context);
      HttpRequest request = requestBuilder.build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      IBaseResource resource = FhirContextProvider.getFhirContext().newNDJsonParser().parseResource(response.body());
      parsedData.put(output.getType(), resource);
    }

    for (Map.Entry<String, IBaseResource> dataItem : parsedData.entrySet()) {
      if (Objects.equals(dataItem.getKey(), "Patient")) {

        QueryList queryList = tenantService.getConfig().getQueryList();

        if (queryList == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant is not configured to query the EHR for patient lists");
        }

        for(var ehrPatientList : queryList.getLists()){
          for(String measureId : ehrPatientList.getMeasureId()){
            PatientList patientList = new PatientList();
            ReportingPeriodCalculator calculator = new ReportingPeriodCalculator(ReportingPeriodMethods.CurrentMonth);

            patientList.setLastUpdated(new Date());
            patientList.setPeriodStart(calculator.getStart());
            patientList.setPeriodEnd(calculator.getEnd());


            Bundle bundle = (Bundle)dataItem.getValue();
            patientList.setMeasureId(measureId);

            bundle.getEntry().forEach(entry -> {
              if(entry.getResource() instanceof Patient){
                Patient patient = (Patient)entry.getResource();
                PatientId patientId = new PatientId();
                patientId.setIdentifier(patient.getId());
                patientId.setReference(patient.getIdentifier().stream().findFirst().get().getSystem() + "|" + patient.getIdentifier().stream().findFirst().get().getValue());
                patientList.getPatients().add(patientId);
              }
            });

            PatientIdService patientIdService = new PatientIdService();
            patientIdService.setContext(context);
            patientIdService.storePatientList(tenantService, patientList);
          }
        }
      }
    }
  }

  private void setAuthHeaders(HttpRequest.Builder requestBuilder, TenantService tenantService, ApplicationContext context) throws Exception {
    Class<?> authClass = Class.forName(tenantService.getConfig().getFhirQuery().getAuthClass());
    var authorizer = (ICustomAuth) context.getBean(authClass);
    authorizer.setTenantService(tenantService);

    String apiKey = authorizer.getApiKeyHeader();
    String authHeader = authorizer.getAuthHeader();

    if (authHeader != null && !authHeader.isEmpty()) {
      requestBuilder.setHeader("Authorization", Helper.sanitizeHeader(authHeader));
    }
    if (apiKey != null && !apiKey.isEmpty()) {
      requestBuilder.setHeader("apikey", Helper.sanitizeHeader(apiKey));
    }
  }
}
