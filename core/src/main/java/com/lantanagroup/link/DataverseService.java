package com.lantanagroup.link;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.model.tenant.GenerateReport;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.db.model.tenant.auth.BasicAuth;
import com.lantanagroup.link.db.model.tenant.auth.BasicAuthAndApiKey;
import com.lantanagroup.link.db.model.tenant.auth.CernerAuth;
import com.lantanagroup.link.db.model.tenant.auth.EpicAuth;
import com.lantanagroup.link.model.DataverseScheduledReport;
import com.lantanagroup.link.model.DataverseScheduledReports;
import com.lantanagroup.link.model.DataverseTenant;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DataverseService {
  private static final Logger logger = LoggerFactory.getLogger(DataverseService.class);

  private static final String cronRegex = "^(\\*|[0-9]{1,2})(/[0-9]+)?(\\s+(\\*|[0-9]{1,2})(/[0-9]+)?)?(\\s+(\\*|[0-9]{1,2})(/[0-9]+)?)?(\\s+(\\*|[0-9]{1,2})(/[0-9]+)?)?(\\s+(\\*|[0-9]{1,2})(/[0-9]+)?)?(\\s+(\\*|[0-9]{1,2})(/[0-9]+)?)?$";

  private ApiConfig apiConfig;

  private String dataverseTenantId;

  private HttpClient client = HttpClient.newHttpClient();

  private ObjectMapper mapper = new ObjectMapper();

  private String token;

  public DataverseService(ApiConfig apiConfig, String dataverseTenantId) {
    this.apiConfig = apiConfig;
    this.dataverseTenantId = dataverseTenantId;
    this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    this.token = this.authenticate();
  }

  private String authenticate() {
    URI tokenEndpoint = null;

    if (this.apiConfig.getDataverse() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Dataverse configuration is missing");
    }

    try {
      tokenEndpoint = new URI(this.apiConfig.getDataverse().getTokenEndpoint());
    } catch (URISyntaxException e) {
      logger.error("Configured dataverse token endpoint couldn't be parsed", e);
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Configured dataverse token endpoint couldn't be parsed");
    }

    String body =
            "grant_type=client_credentials&" +
            "client_id=" + this.apiConfig.getDataverse().getClientId() + "&" +
            "client_secret=" + this.apiConfig.getDataverse().getClientSecret() + "&" +
            "resource=https://" + this.apiConfig.getDataverse().getOrgId() + ".crm.dynamics.com";
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(tokenEndpoint);
    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
    HttpRequest request = requestBuilder.build();
    HttpResponse<String> response;

    try {
      response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      logger.error("Failed to authenticate with dataverse", e);
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Failed to authenticate with dataverse");
    }

    if (response.statusCode() != 200) {
      logger.error("Failed to authenticate with dataverse: " + response.body());
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Failed to authenticate with dataverse");
    }

    String json = response.body();
    int start = json.indexOf("access_token\":\"") + 15;
    int end = json.indexOf("\"", start);
    return json.substring(start, end);
  }

  private <T> T getDataverseByUrl(String url, TypeReference<T> type) {
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
    requestBuilder.GET();
    requestBuilder.uri(URI.create(url));
    requestBuilder.setHeader("Authorization", "Bearer " + this.token);
    HttpRequest request = requestBuilder.build();
    HttpResponse<String> response;

    try {
      response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      logger.error("Failed to retrieve from dataverse " + url, e);
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Failed to retrieve from dataverse");
    }

    if (response.statusCode() != 200) {
      logger.error("Failed to retrieve from dataverse " + url + ": " + response.body());
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Failed to retrieve from dataverse");
    }

    String json = response.body();

    try {
      return this.mapper.readValue(json, type);
    } catch (JsonProcessingException e) {
      logger.error("Failed to parse/deserialize JSON returned from dataverse for tenant config", e);
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Failed to parse/deserialize JSON returned from dataverse for tenant config");
    }
  }

  private <T> T getDataverseById(String objectName, String id, TypeReference<T> type) {
    String url = "https://" + this.apiConfig.getDataverse().getOrgId() + ".crm.dynamics.com/api/data/v9.1/" + objectName + "(" + id + ")";
    return this.getDataverseByUrl(url, type);
  }

  private <T> T getDataverseByFilter(String objectName, String filterProperty, String filterValue, TypeReference<T> type) {
    String url = "https://" + this.apiConfig.getDataverse().getOrgId() + ".crm.dynamics.com/api/data/v9.1/" + objectName + "?$filter=" + filterProperty + "%20eq%20%27" + filterValue + "%27";
    return this.getDataverseByUrl(url, type);
  }

  private void populateTenant(Tenant tenant) {
    DataverseTenant dvTenant = this.getDataverseById("crcbb_linktenantconfigs", this.dataverseTenantId, new TypeReference<>() {});

    if (StringUtils.isNotEmpty(dvTenant.getName())) {
      tenant.setName(dvTenant.getName());
    } else {
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing name in dataverse tenant");
    }

    if (StringUtils.isNotEmpty(dvTenant.getNameInBundle())) {
      tenant.getBundling().setName(dvTenant.getNameInBundle());
    } else {
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing name within bundle in dataverse tenant");
    }

    if (StringUtils.isNotEmpty(dvTenant.getDescription())) {
      tenant.setDescription(dvTenant.getDescription());
    }

    if (StringUtils.isNotEmpty(dvTenant.getNhsnOrgId())) {
      tenant.setCdcOrgId(dvTenant.getNhsnOrgId());
    } else {
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing NHSN Org ID in dataverse tenant");
    }

    if (dvTenant.getNormalizeCodeSystemCleanup()) {
      tenant.getEvents().getAfterPatientDataQuery().add("com.lantanagroup.link.events.CodeSystemCleanup");
    }

    if (dvTenant.getNormalizeEncounterStatusTransformer()) {
      tenant.getEvents().getAfterPatientDataQuery().add("com.lantanagroup.link.events.EncounterStatusTransformer");
    }

    if (dvTenant.getNormalizeContainedResourceCleanup()) {
      tenant.getEvents().getAfterPatientDataQuery().add("com.lantanagroup.link.events.ContainedResourceCleanup");
    }

    if (dvTenant.getNormalizeCopyLocationIdentifierToType()) {
      tenant.getEvents().getAfterPatientDataQuery().add("com.lantanagroup.link.events.CopyLocationIdentifierToType");
    }

    if (dvTenant.getNormalizeFixPeriodDates()) {
      tenant.getEvents().getAfterPatientDataQuery().add("com.lantanagroup.link.events.FixPeriodDates");
    }

    if (dvTenant.getNormalizeFixResourceIds()) {
      tenant.getEvents().getAfterPatientDataQuery().add("com.lantanagroup.link.events.FixResourceId");
    }

    if (dvTenant.getNormalizePatientDataResourceFilter()) {
      tenant.getEvents().getAfterPatientDataQuery().add("com.lantanagroup.link.events.PatientDataResourceFilter");
    }

    if (StringUtils.isNotEmpty(dvTenant.getScheduledDataRetentionCheck())) {
      if (!Pattern.matches(cronRegex, dvTenant.getScheduledDataRetentionCheck())) {
        throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Invalid cron expression for data retention check: " + dvTenant.getScheduledDataRetentionCheck());
      }
      tenant.getScheduling().setDataRetentionCheckCron(dvTenant.getScheduledDataRetentionCheck());
    }

    if (StringUtils.isNotEmpty(dvTenant.getScheduledQuerySTU3PatientList())) {
      if (!Pattern.matches(cronRegex, dvTenant.getScheduledQuerySTU3PatientList())) {
        throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Invalid cron expression for query patient list: " + dvTenant.getScheduledQuerySTU3PatientList());
      }
      tenant.getScheduling().setQueryPatientListCron(dvTenant.getScheduledQuerySTU3PatientList());
    }

    if (dvTenant.getAuthenticationMethod() != null) {
      switch (dvTenant.getAuthenticationMethod()) {
        case 1:
          tenant.getFhirQuery().setAuthClass("com.lantanagroup.link.auth.EpicAuth");
          tenant.getFhirQuery().setEpicAuth(new EpicAuth());

          if (StringUtils.isNotEmpty(dvTenant.getAuthClientId())) {
            tenant.getFhirQuery().getEpicAuth().setClientId(dvTenant.getAuthClientId());
          } else {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing auth client ID in dataverse tenant");
          }

          if (StringUtils.isNotEmpty(dvTenant.getAuthTokenUrl())) {
            tenant.getFhirQuery().getEpicAuth().setTokenUrl(dvTenant.getAuthTokenUrl());
          } else {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing auth token URL in dataverse tenant");
          }

          if (StringUtils.isNotEmpty(dvTenant.getAuthAudience())) {
            tenant.getFhirQuery().getEpicAuth().setAudience(dvTenant.getAuthAudience());
          } else {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing auth audience in dataverse tenant");
          }
          break;
        case 2:
          tenant.getFhirQuery().setAuthClass("com.lantanagroup.link.auth.CernerAuth");
          tenant.getFhirQuery().setCernerAuth(new CernerAuth());

          if (StringUtils.isNotEmpty(dvTenant.getAuthClientId())) {
            tenant.getFhirQuery().getCernerAuth().setClientId(dvTenant.getAuthClientId());
          } else {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing auth client ID in dataverse tenant");
          }

          if (StringUtils.isNotEmpty(dvTenant.getAuthTokenUrl())) {
            tenant.getFhirQuery().getCernerAuth().setTokenUrl(dvTenant.getAuthTokenUrl());
          } else {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing auth token URL in dataverse tenant");
          }

          if (StringUtils.isNotEmpty(dvTenant.getAuthAudience())) {
            tenant.getFhirQuery().getCernerAuth().setScopes(dvTenant.getAuthAudience());    // Weird... but works for now
          } else {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing auth audience in dataverse tenant");
          }
          break;
        case 3:
          tenant.getFhirQuery().setAuthClass("com.lantanagroup.link.auth.BasicAuth");
          tenant.getFhirQuery().setBasicAuth(new BasicAuth());

          if (StringUtils.isNotEmpty(dvTenant.getAuthClientId())) {
            tenant.getFhirQuery().getBasicAuth().setUsername(dvTenant.getAuthClientId());     // kinda weird... but will work for now
          } else {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing auth client ID (username) in dataverse tenant");
          }
          break;
        case 4:
          tenant.getFhirQuery().setAuthClass("com.lantanagroup.link.auth.BasicAuthAndApiKey");
          tenant.getFhirQuery().setBasicAuthAndApiKey(new BasicAuthAndApiKey());

          if (StringUtils.isNotEmpty(dvTenant.getAuthClientId())) {
            tenant.getFhirQuery().getBasicAuthAndApiKey().setUsername(dvTenant.getAuthClientId());     // kinda weird... but will work for now
          } else {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing auth client ID (username) in dataverse tenant");
          }
          break;
        default:
          throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Unknown authentication method: " + dvTenant.getAuthenticationMethod());
      }
    } else {
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing authentication method in dataverse tenant");
    }

    if (StringUtils.isNotEmpty(dvTenant.getFhirServerBase())) {
      tenant.getFhirQuery().setFhirServerBase(dvTenant.getFhirServerBase());
    } else {
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing FHIR server base in dataverse tenant");
    }

    if (dvTenant.getParallelPatients() != null) {
      tenant.getFhirQuery().setParallelPatients(dvTenant.getParallelPatients());
    } else {
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing parallel patient count in dataverse tenant");
    }
  }

  private List<String> getMeasuresById(List<String> ids) {
    return ids.stream().map(id -> {
      Integer dqmId = Integer.parseInt(id);

      if (this.apiConfig.getDataverse().getDqms().get(dqmId) == null) {
        throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Unknown/un-configured DQM ID: " + dqmId);
      }

      return this.apiConfig.getDataverse().getDqms().get(dqmId);
    }).collect(Collectors.toList());
  }

  private void populateScheduledReports(Tenant tenant) {
    DataverseScheduledReports dvScheduledReports = this.getDataverseByFilter("lcg_linkschedulegenerateandsubmits", "_lcg_crcbb_linktenantconfig_value", this.dataverseTenantId, new TypeReference<DataverseScheduledReports>() {});

    if (dvScheduledReports.getValue() == null) {
      return;
    }

    for (DataverseScheduledReport dvScheduledReport : dvScheduledReports.getValue()) {
      GenerateReport generateReport = new GenerateReport();
      tenant.getScheduling().getGenerateAndSubmitReports().add(generateReport);

      if (dvScheduledReport.getReportingPeriodMethod() == null) {
        throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing reporting period method for scheduled report");
      }

      if (dvScheduledReport.getSchedule() == null) {
        throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing schedule for scheduled report");
      } else if (!Pattern.matches(cronRegex, dvScheduledReport.getSchedule())) {
        throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Invalid cron expression for scheduled report: " + dvScheduledReport.getSchedule());
      }

      if (dvScheduledReport.getMeasures() == null) {
        throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Missing measures for scheduled report");
      }

      generateReport.setCron(dvScheduledReport.getSchedule());
      generateReport.setRegenerateIfExists(dvScheduledReport.getRegenerate());
      generateReport.setMeasureIds(this.getMeasuresById(List.of(dvScheduledReport.getMeasures().split(","))));

      switch (dvScheduledReport.getReportingPeriodMethod()) {
        case 2:
          generateReport.setReportingPeriodMethod(ReportingPeriodMethods.LastMonth);
          break;
        case 4:
          generateReport.setReportingPeriodMethod(ReportingPeriodMethods.LastWeek);
          break;
        case 1:
          generateReport.setReportingPeriodMethod(ReportingPeriodMethods.CurrentMonth);
          break;
        case 3:
          generateReport.setReportingPeriodMethod(ReportingPeriodMethods.CurrentWeek);
          break;
        default:
          throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Unknown reporting period method: " + dvScheduledReport.getReportingPeriodMethod());
      }
    }
  }

  /**
   * Updates the tenant provided based on the data in the dataverse tenant with the provided ID.
   */
  public Tenant getTenant() {
    this.token = authenticate();

    Tenant tenant = new Tenant();
    tenant.setId(this.dataverseTenantId);

    this.populateTenant(tenant);
    this.populateScheduledReports(tenant);

    return tenant;
  }
}
