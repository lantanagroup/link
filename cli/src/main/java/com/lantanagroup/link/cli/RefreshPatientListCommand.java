package com.lantanagroup.link.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiReportDefsUrlConfig;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.query.auth.HapiFhirAuthenticationInterceptor;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.IOException;

@ShellComponent
public class RefreshPatientListCommand {
  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private RefreshPatientListConfig config;

  @Autowired
  private QueryConfig queryConfig;

  @Autowired
  private ApiConfig apiConfig;

  private final HttpClient httpClient = HttpClients.createDefault();
  private final FhirContext fhirContext = FhirContextProvider.getFhirContext();

  @ShellMethod(
          key = "refresh-patient-list",
          value = "Read an Epic patient list and update the corresponding census in Link.")
  public void execute() throws Exception {
    if (config.getApiUrl() == null) {
      throw new IllegalArgumentException("api-url may not be null");
    }
    if (config.getAuth() == null) {
      throw new IllegalArgumentException("auth may not be null");
    }
    if (config.getPatientListId() == null) {
      throw new IllegalArgumentException("patient-list-id may not be null");
    }
    ApiReportDefsUrlConfig urlConfig = getUrlConfig();
    if (urlConfig.getCensusIdentifier() == null) {
      throw new IllegalArgumentException("census-identifier may not be null");
    }
    ListResource source = getList();
    ListResource target = transformList(source, urlConfig.getCensusIdentifier());
    postList(target);
  }

  private ApiReportDefsUrlConfig getUrlConfig() {
    for (ApiReportDefsUrlConfig urlConfig : apiConfig.getReportDefs().getUrls()) {
      if (config.getPatientListId().equals(urlConfig.getPatientListId())) {
        return urlConfig;
      }
    }
    throw new IllegalArgumentException("patient-list-id not found");
  }

  private ListResource getList() throws ClassNotFoundException {
    fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    IGenericClient client = fhirContext.newRestfulGenericClient(queryConfig.getFhirServerBase());
    client.registerInterceptor(new HapiFhirAuthenticationInterceptor(queryConfig, applicationContext));
    return client.fetchResourceFromUrl(
            ListResource.class,
            URLEncodedUtils.formatSegments("STU3", "List", config.getPatientListId()));
  }

  private ListResource transformList(ListResource source, String censusIdentifier) {
    ListResource target = new ListResource();
    Period period = new Period()
            .setStart(Helper.getStartOfMonth(source.getDate()))
            .setEnd(Helper.getEndOfMonth(source.getDate(), 0));
    target.addExtension(Constants.ApplicablePeriodExtensionUrl, period);
    target.addIdentifier()
            .setSystem(Constants.MainSystem)
            .setValue(censusIdentifier);
    target.setStatus(ListResource.ListStatus.CURRENT);
    target.setMode(ListResource.ListMode.WORKING);
    target.setTitle(String.format("Census List for %s", censusIdentifier));
    target.setCode(source.getCode());
    target.setDate(source.getDate());
    target.setEntry(source.getEntry());
    return target;
  }

  private void postList(ListResource list) throws IOException {
    AuthConfig authConfig = config.getAuth();
    String token = OAuth2Helper.getPasswordCredentialsToken(
            httpClient,
            authConfig.getTokenUrl(),
            authConfig.getUser(),
            authConfig.getPass(),
            "nhsnlink-app",
            authConfig.getScope());
    if (token == null) {
      throw new IOException("Authentication failed");
    }
    String path = URLEncodedUtils.formatSegments("poi", "fhir", "List");
    String url = String.format("%s/%s", config.getApiUrl(), path);
    HttpPost request = new HttpPost(url);
    request.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token));
    request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
    String entity = fhirContext.newJsonParser().encodeResourceToString(list);
    request.setEntity(new StringEntity(entity));
    httpClient.execute(request, response -> {
      System.out.println(response);
      return null;
    });
  }
}
