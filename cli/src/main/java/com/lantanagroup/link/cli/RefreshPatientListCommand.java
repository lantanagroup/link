package com.lantanagroup.link.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.query.auth.EpicAuth;
import com.lantanagroup.link.query.auth.EpicAuthConfig;
import com.lantanagroup.link.query.auth.HapiFhirAuthenticationInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

@ShellComponent
public class RefreshPatientListCommand extends BaseShellCommand {
  private static final Logger logger = LoggerFactory.getLogger(RefreshPatientListCommand.class);

  private final CloseableHttpClient httpClient = HttpClients.createDefault();
  private final FhirContext fhirContext = FhirContextProvider.getFhirContext();
  private RefreshPatientListConfig config;
  private QueryConfig queryConfig;
  private USCoreConfig usCoreConfig;


  @Override
  protected List<Class<?>> getBeanClasses() {

    return List.of(
            QueryConfig.class,
            USCoreConfig.class,
            EpicAuth.class,
            EpicAuthConfig.class);
  }

  @ShellMethod(
          key = "refresh-patient-list",
          value = "Read patient lists and update the corresponding census in Link.")
  public void execute(@ShellOption(defaultValue = "") String[] patientListPath) throws Exception {
    registerBeans();
    config = applicationContext.getBean(RefreshPatientListConfig.class);
    queryConfig = applicationContext.getBean(QueryConfig.class);
    usCoreConfig = applicationContext.getBean(USCoreConfig.class);

    List<RefreshPatientListConfig.PatientList> filteredList = config.getPatientList();
    // if patientListPath argument is not passed to the command then load all the lists defined in the configuration file
    if (patientListPath.length > 0) {
      filteredList = config.getPatientList().stream().filter(item -> List.of(patientListPath).contains(item.getPatientListPath())).collect(Collectors.toList());
    }
    for (RefreshPatientListConfig.PatientList listResource : filteredList) {
      ListResource source = readList(listResource.getPatientListPath());
      for (int j = 0; j < listResource.getCensusIdentifier().size(); j++) {
        ListResource target = transformList(source, listResource.getCensusIdentifier().get(j));
        updateList(target);
      }
    }
  }

  private ListResource readList(String patientListId) throws ClassNotFoundException {
    fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    IGenericClient client = fhirContext.newRestfulGenericClient(usCoreConfig.getFhirServerBase());
    client.registerInterceptor(new HapiFhirAuthenticationInterceptor(queryConfig, applicationContext));
    return client.fetchResourceFromUrl(ListResource.class, patientListId);
  }

  private ListResource transformList(ListResource source, String censusIdentifier) throws URISyntaxException {
    logger.info("Transforming");
    ListResource target = new ListResource();
    Period period = new Period();

    if (this.config.getCensusReportingPeriod().equals(CensusReportingPeriods.Month)) {
      period
              .setStart(Helper.getStartOfMonth(source.getDate()))
              .setEnd(Helper.getEndOfMonth(source.getDate(), 0));
    } else if (this.config.getCensusReportingPeriod().equals(CensusReportingPeriods.Day)) {
      period
              .setStart(Helper.getStartOfDay(source.getDate()))
              .setEnd(Helper.getEndOfDay(source.getDate(), 0));
    }

    target.addExtension(Constants.ApplicablePeriodExtensionUrl, period);
    target.addIdentifier()
            .setSystem(Constants.MainSystem)
            .setValue(censusIdentifier);
    target.setStatus(ListResource.ListStatus.CURRENT);
    target.setMode(ListResource.ListMode.WORKING);
    target.setTitle(String.format("Census List for %s", censusIdentifier));
    target.setCode(source.getCode());
    target.setDate(source.getDate());
    URI baseUrl = new URI(usCoreConfig.getFhirServerBase());
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

  private void updateList(ListResource target) throws Exception {
    String url = String.format("%s/poi/fhir/List", config.getApiUrl());
    logger.info("Submitting to {}", url);
    HttpPost request = new HttpPost(url);
    if (config.getAuth() != null && config.getAuth().getCredentialMode() != null) {
      String token = null;

      ///TODO: Potentially change this to a implementation of an interface instead of using the helper class
      if(StringUtils.equalsIgnoreCase(config.getAuth().getCredentialMode(), "password")) {
        token = OAuth2Helper.getPasswordCredentialsToken(
                httpClient,
                config.getAuth().getTokenUrl(),
                config.getAuth().getUser(),
                config.getAuth().getPass(),
                config.getAuth().getClientId(),
                config.getAuth().getScope());
      }
      else if(StringUtils.equalsIgnoreCase(config.getAuth().getCredentialMode(), "sams-password")) {
        token = OAuth2Helper.getSamsPasswordCredentialsToken(
                httpClient,
                config.getAuth().getTokenUrl(),
                config.getAuth().getUser(),
                config.getAuth().getPass(),
                config.getAuth().getClientId(),
                config.getAuth().getClientSecret(),
                config.getAuth().getScope());
      }
      else if (StringUtils.equalsIgnoreCase(config.getAuth().getCredentialMode(), "client")) {
        token = OAuth2Helper.getClientCredentialsToken(
                httpClient,
                config.getAuth().getTokenUrl(),
                config.getAuth().getClientId(),
                config.getAuth().getPass(),
                config.getAuth().getScope());
      }

      if (token == null) {
        throw new Exception("Authorization failed");
      }

      if (OAuth2Helper.validateHeaderJwtToken(token)) {
        request.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token));
      } else {
        throw new JWTVerificationException("Invalid token format");
      }

    }
    request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
    request.setEntity(new StringEntity(fhirContext.newJsonParser().encodeResourceToString(target)));
    httpClient.execute(request, response -> {
      logger.info("Response: {}", response.getStatusLine());
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        String body = EntityUtils.toString(entity);
        if (StringUtils.isNotEmpty(body)) {
          logger.debug(body);
        }
      }
      return null;
    });
  }
}
