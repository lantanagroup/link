package com.lantanagroup.link.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.lantanagroup.link.*;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiReportDefsUrlConfig;
import com.lantanagroup.link.config.auth.LinkOAuthConfig;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Meta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApiInit {
  private static final Logger logger = LoggerFactory.getLogger(ApiInit.class);
  @Setter
  protected FhirContext ctx = FhirContextProvider.getFhirContext();
  @Autowired
  private ApiConfig config;
  @Autowired
  private QueryConfig queryConfig;

  @Autowired
  private USCoreConfig usCoreConfig;

  @Autowired
  private FhirDataProvider provider;

  @Value("classpath:fhir/*")
  private Resource[] resources;


  private boolean checkPrerequisites() {

    logger.info("Checking that API prerequisite services are available. maxRetry: %d, retryWait: %d", config.getMaxRetry(), config.getRetryWait());

    boolean allServicesAvailable = false;
    boolean dataStoreAvailable = false;
    boolean terminologyServiceAvailable = false;
    boolean evaluationServiceAvailable = false;

    for (int retry = 0; config.getMaxRetry() == null || retry <= config.getMaxRetry(); retry++) {

      // Check data store availability
      if (!dataStoreAvailable) {
        try {
          CapabilityStatement cs = new FhirDataProvider(config.getDataStore()).getClient().capabilities().ofType(CapabilityStatement.class).execute();
          logger.info(String.format("CapabilityStatement: %s -- %d", cs.getUrl(), cs.getRest().size()));
          dataStoreAvailable = true;
        } catch (BaseServerResponseException e) {
          logger.error(String.format("Could not connect to data store %s (%s)", config.getDataStore().getBaseUrl(), e));
        }
      }

      // Check terminology service availability
      if (!terminologyServiceAvailable) {
        try {
          new FhirDataProvider(config.getTerminologyService()).getClient().capabilities().ofType(CapabilityStatement.class).execute();
          terminologyServiceAvailable = true;
        } catch (BaseServerResponseException e) {
          logger.error(String.format("Could not connect to terminology service %s (%s)", config.getTerminologyService(), e));
        }
      }

      // Check evaluation service availability
      if (!evaluationServiceAvailable) {
        try {
          new FhirDataProvider(config.getEvaluationService()).getClient().capabilities().ofType(CapabilityStatement.class).execute();
          evaluationServiceAvailable = true;
        } catch (BaseServerResponseException e) {
          logger.error(String.format("Could not connect to evaluation service %s (%s)", config.getEvaluationService(), e));
        }
      }


      // Check if all services are now available
      allServicesAvailable = dataStoreAvailable && terminologyServiceAvailable && evaluationServiceAvailable;
      if (allServicesAvailable) {
        logger.info("All prerequisite services in API init are available.");
        break;
      }

      try {
        Thread.sleep(config.getRetryWait());
      } catch (InterruptedException ignored) {
      }
    }

    // Not all prerequisite services are available... cannot continue
    if (!allServicesAvailable) {
      logger.error(String.format("API prerequisite services are not available.  Availability: data-store: %s, terminology-service: %s, evaluation-service: %s",
              dataStoreAvailable, terminologyServiceAvailable, evaluationServiceAvailable));
      return false;
    }

    return true;
  }

  private void loadMeasureDefinitions() {
    HttpClient client = HttpClient.newHttpClient();
    config.getReportDefs().getUrls().parallelStream().forEach(urlConfig -> {
      String bundleId = urlConfig.getBundleId();
      logger.info(String.format("Loading report def for %s", bundleId));
      Bundle localBundle = null;
      try {
        localBundle = provider.getBundleById(bundleId);
      } catch (ResourceNotFoundException ignored) {
      }
      try {
        loadMeasureDefinition(client, urlConfig, localBundle);
      } catch (Exception e) {
        logger.error(String.format("Error loading report def for %s", bundleId), e);
      }
    });
  }

  public Bundle loadMeasureDefinition(
          HttpClient client,
          ApiReportDefsUrlConfig urlConfig,
          Bundle localBundle)
          throws Exception {

    // Check that URL is HTTPS if required
    URI uri = new URI(urlConfig.getUrl());
    if (config.isRequireHttps() && !"https".equalsIgnoreCase(uri.getScheme())) {
      throw new IllegalStateException("URL is not HTTPS");
    }
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri);

    // Add If-Modified-Since header based on local bundle
    if (localBundle != null && localBundle.getMeta().hasLastUpdated()) {
      SimpleDateFormat format = new SimpleDateFormat(Helper.RFC_1123_DATE_TIME_FORMAT);
      Date lastUpdated = localBundle.getMeta().getLastUpdated();
      requestBuilder.setHeader("If-Modified-Since", format.format(lastUpdated));
    }

    // Add Authorization header if configured
    LinkOAuthConfig authConfig = config.getReportDefs().getAuth();
    if (authConfig != null) {
      String token = OAuth2Helper.getToken(authConfig);
      if (OAuth2Helper.validateHeaderJwtToken(token)) {
        requestBuilder.setHeader("Authorization", "Bearer " + token);
      } else {
        throw new JWTVerificationException("Invalid token format");
      }
    }

    // Retrieve remote bundle, retrying if necessary
    HttpRequest request = requestBuilder.build();
    HttpResponse<String> response = null;
    for (int retry = 1; retry <= config.getReportDefs().getMaxRetry(); retry++) {
      logger.debug("Retrieving report def");
      try {
        response = client.send(request, HttpResponse.BodyHandlers.ofString());
        break;
      } catch (IOException e) {
        logger.warn(String.format("Error retrieving report def: %s", e.getMessage()));
      }
      int retryWait = config.getReportDefs().getRetryWait();
      logger.debug(String.format("Retrying in %d milliseconds", retryWait));
      try {
        Thread.sleep(retryWait);
      } catch (InterruptedException ignored) {
      }
    }
    if (response == null || (response.statusCode() != 200 && response.statusCode() != 304)) {
      throw new Exception("Failed to retrieve report def");
    }

    // Return local bundle if up to date
    if (response.statusCode() == 304) {
      logger.debug("Report def is up to date; not storing");
      return localBundle;
    }

    // Parse and validate remote bundle
    Bundle remoteBundle = FhirHelper.parseResource(Bundle.class, response.body());
    if (!FhirHelper.validLibraries(remoteBundle)) {
      throw new Exception("Report def contains libraries without data requirements");
    }
    List<String> missingResourceTypes = FhirHelper.getQueryConfigurationDataReqMissingResourceTypes(
            FhirHelper.getQueryConfigurationResourceTypes(usCoreConfig),
            remoteBundle);
    if (!missingResourceTypes.isEmpty()) {
      logger.warn(String.format(
              "Report def contains data requirements that are not configured for querying: %s",
              String.join(", ", missingResourceTypes)));
    }


    // Store to terminology service
    logger.debug("Storing to terminology service");
    Bundle evaluationBundle = FhirHelper.storeTerminologyAndReturnOther(remoteBundle, config);

    // Set ID, meta
    String bundleId = urlConfig.getBundleId();
    evaluationBundle.setId(bundleId);
    evaluationBundle.setMeta(localBundle != null
            ? localBundle.getMeta()
            : new Meta().addTag(Constants.MainSystem, Constants.ReportDefinitionTag, null));

    Measure measure = FhirHelper.getMeasure(remoteBundle);
    if (!measure.hasIdentifier()) {
      logger.error(String.format("Measure : %s does not have an identifier.", measure.getId()));
      measure.addIdentifier().setSystem(Constants.MainSystem).setValue(bundleId);
    }

    // Store to evaluation service and internal data store
    logger.debug("Storing to evaluation service");
    FhirDataProvider evaluationProvider = new FhirDataProvider(config.getEvaluationService());
    evaluationProvider.transaction(evaluationBundle);
    logger.debug("Storing to internal data store");
    provider.updateResource(evaluationBundle);
    return evaluationBundle;
  }

  private void loadSearchParameters() {
    try {
      FhirContext ctx = FhirContextProvider.getFhirContext();
      IParser xmlParser = ctx.newXmlParser();
      logger.info(String.format("Resources count: %d", resources.length));
      for (final Resource res : resources) {
        try (InputStream inputStream = res.getInputStream();) {
          IBaseResource resource = readFileAsFhirResource(xmlParser, inputStream);
          provider.updateResource(resource);
        }
      }
    } catch (Exception ex) {
      logger.error(String.format("Error in loadSearchParameters due to %s", ex.getMessage()));
    }
  }

  private IBaseResource readFileAsFhirResource(IParser xmlParser, InputStream file) {
    String resourceString = new BufferedReader(new InputStreamReader(file)).lines().parallel().collect(Collectors.joining("\n"));
    return xmlParser.parseResource(resourceString);
  }

  private int getSocketTimout() {
    int socketTimeout = 30 * 1000; // 30 sec // 200 * 5000
    if (config.getSocketTimeout() != null) {
      try {
        socketTimeout = Integer.parseInt(config.getSocketTimeout());
      } catch (Exception ex) {
        logger.error(String.format("Error % in socket-timeout %s ", ex.getMessage(), config.getSocketTimeout()));
      }
    }
    return socketTimeout;
  }

  public void init() {
    this.ctx.getRestfulClientFactory().setSocketTimeout(getSocketTimout());

    Optional<ApiReportDefsUrlConfig> measureReportAggregator = config.getReportDefs().getUrls().stream().filter(urlConfig -> StringUtils.isEmpty(urlConfig.getReportAggregator())).findFirst();
    if (StringUtils.isEmpty(config.getReportAggregator()) && !measureReportAggregator.isEmpty()) {
      String msg = "Not all measures have aggregators configured and there is no default aggregator in the configuration file.";
      logger.error(msg);
      throw new IllegalStateException(msg);
    }

    if (this.queryConfig.isRequireHttps() && !this.usCoreConfig.getFhirServerBase().toLowerCase().startsWith("https://")) {
      throw new IllegalStateException("Error, Query URL requires https");
    }

    if (this.config.getSkipInit()) {
      logger.info("Skipping API initialization processes to load report defs and search parameters");
      return;
    }

    if (this.config.getValidateFhirServer() != null && !this.config.getValidateFhirServer()) {
      this.ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
      logger.info("Setting client to never query for metadata");
    }


    // check that prerequisite services are available
    if (!this.checkPrerequisites()) {
      throw new IllegalStateException("Prerequisite services check failed. Cannot continue API initialization.");
    }

    this.loadMeasureDefinitions();
    this.loadSearchParameters();

  }

}
