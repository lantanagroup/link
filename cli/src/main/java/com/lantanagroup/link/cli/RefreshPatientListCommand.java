package com.lantanagroup.link.cli;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiReportDefsUrlConfig;
import com.lantanagroup.link.config.query.QueryConfig;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

@ShellComponent
public class RefreshPatientListCommand {
  @Autowired
  private RefreshPatientListConfig config;

  @Autowired
  private QueryConfig queryConfig;

  @Autowired
  private ApiConfig apiConfig;

  private final HttpClient httpClient = HttpClients.createDefault();
  private final FhirContext fhirContext = FhirContextProvider.getFhirContext();

  @ShellMethod("refresh-patient-list")
  public void refreshPatientList() throws IOException {
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
    if (urlConfig == null) {
      throw new IllegalArgumentException("patient-list-id not found");
    }
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
    return null;
  }

  private ListResource getList() throws IOException {
    String path = URLEncodedUtils.formatSegments("api", "FHIR", "STU3", "List", config.getPatientListId());
    String url = String.format("%s/%s", queryConfig.getFhirServerBase(), path);
    HttpGet request = new HttpGet(url);
    request.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
    return httpClient.execute(request, response -> {
      String entity = EntityUtils.toString(response.getEntity());
      return fhirContext.newJsonParser().parseResource(ListResource.class, entity);
    });
  }

  private ListResource transformList(ListResource source, String censusIdentifier) {
    ListResource target = new ListResource();
    Period period = new Period()
            .setStart(getStartOfMonth(source.getDate()))
            .setEnd(getEndOfMonth(source.getDate()));
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

  private Date getStartOfMonth(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime();
  }

  private Date getEndOfMonth(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
    calendar.set(Calendar.HOUR_OF_DAY, 23);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime();
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
