package com.lantanagroup.link.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.auth0.jwt.exceptions.JWTVerificationException;
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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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

  private final CloseableHttpClient httpClient = HttpClients.createDefault();
  private final FhirContext fhirContext = FhirContextProvider.getFhirContext();

  @ShellMethod(
          key = "refresh-patient-list",
          value = "Read an Epic patient list and update the corresponding census in Link.")
  public void execute() throws Exception {
    if (config.getApiUrl() == null) {
      throw new IllegalArgumentException("api-url may not be null");
    }
    if (config.getPatientListId() == null) {
      throw new IllegalArgumentException("patient-list-id may not be null");
    }
    ApiReportDefsUrlConfig urlConfig = getUrlConfig();
    if (urlConfig.getCensusIdentifier() == null) {
      throw new IllegalArgumentException("census-identifier may not be null");
    }
    ListResource source = readList();
    ListResource target = transformList(source, urlConfig.getCensusIdentifier());
    updateList(target);
  }

  private ApiReportDefsUrlConfig getUrlConfig() {
    for (ApiReportDefsUrlConfig urlConfig : apiConfig.getReportDefs().getUrls()) {
      if (config.getPatientListId().equals(urlConfig.getPatientListId())) {
        return urlConfig;
      }
    }
    throw new IllegalArgumentException("patient-list-id not found");
  }

  private ListResource readList() throws ClassNotFoundException {
    fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    IGenericClient client = fhirContext.newRestfulGenericClient(queryConfig.getFhirServerBase());
    client.registerInterceptor(new HapiFhirAuthenticationInterceptor(queryConfig, applicationContext));
    return client.fetchResourceFromUrl(
            ListResource.class,
            URLEncodedUtils.formatSegments("STU3", "List", config.getPatientListId()));
  }

  private ListResource transformList(ListResource source, String censusIdentifier) throws URISyntaxException {
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
    URI baseUrl = new URI(queryConfig.getFhirServerBase());
    for (ListResource.ListEntryComponent sourceEntry : source.getEntry()) {
      target.addEntry(transformListEntry(sourceEntry, baseUrl));
    }
    return target;
  }

  private ListResource.ListEntryComponent transformListEntry(ListResource.ListEntryComponent source, URI baseUrl)
          throws URISyntaxException {
    ListResource.ListEntryComponent target = source.copy();
    if (target.getItem().hasReference()) {
      URI referenceUrl = new URI(target.getItem().getReference());
      if (referenceUrl.isAbsolute()) {
        target.getItem().setReference(baseUrl.relativize(referenceUrl).toString());
      }
    }
    return target;
  }

  private void updateList(ListResource target) throws IOException {
    HttpPost request = new HttpPost(String.format("%s/poi/fhir/List", config.getApiUrl()));
    if (config.getAuth() != null) {
      String token = OAuth2Helper.getPasswordCredentialsToken(
              httpClient,
              config.getAuth().getTokenUrl(),
              config.getAuth().getUser(),
              config.getAuth().getPass(),
              "nhsnlink-app",
              config.getAuth().getScope());
      if (token == null) {
        throw new IOException("Authorization failed");
      }

      if(OAuth2Helper.validateHeaderJwtToken(token)) {
        request.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token));
      }
      else {
        throw new JWTVerificationException("Invalid token format");
      }

    }
    request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
    request.setEntity(new StringEntity(fhirContext.newJsonParser().encodeResourceToString(target)));
    httpClient.execute(request, response -> {
      System.out.println(response);
      return null;
    });
  }
}
