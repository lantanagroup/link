package com.lantanagroup.link.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import com.lantanagroup.link.*;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiReportDefsBundleConfig;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    logger.info("Checking that API prerequisite services are available. maxRetry: {}, retryWait: {}", config.getMaxRetry(), config.getRetryWait());

    boolean allServicesAvailable = false;
    boolean dataStoreAvailable = false;
    boolean evaluationServiceAvailable = false;

    for (int retry = 0; config.getMaxRetry() == null || retry <= config.getMaxRetry(); retry++) {

      // Check data store availability
      if (!dataStoreAvailable) {
        try {
          CapabilityStatement cs = new FhirDataProvider(config.getDataStore())
                  .getClient()
                  .capabilities()
                  .ofType(CapabilityStatement.class)
                  .execute();
          logger.info(String.format("Data Store at %s, FHIR Version: %s, Implementation: %s, Software: %s %s",
                  config.getDataStore().getBaseUrl(),
                  cs.getFhirVersion().toString(),
                  cs.getImplementation().getDescription(),
                  cs.getSoftware().getName(),
                  cs.getSoftware().getVersion()));
          dataStoreAvailable = true;
        } catch (BaseServerResponseException e) {
          logger.error(String.format("Could not connect to data store %s (%s)", config.getDataStore().getBaseUrl(), e));
        }
      }

      // Check evaluation service availability
      if (!evaluationServiceAvailable) {
        try {
          CapabilityStatement cs = new FhirDataProvider(config.getEvaluationService()).getClient().capabilities().ofType(CapabilityStatement.class).execute();
          logger.info(String.format("Evaluation Service at %s, FHIR Version: %s, Implementation: %s, Software: %s %s",
                  config.getEvaluationService(),
                  cs.getFhirVersion().toString(),
                  cs.getImplementation().getDescription(),
                  cs.getSoftware().getName(),
                  cs.getSoftware().getVersion()));
          evaluationServiceAvailable = true;
        } catch (BaseServerResponseException e) {
          logger.error(String.format("Could not connect to evaluation service %s (%s)", config.getEvaluationService(), e));
        }
      }


      // Check if all services are now available
      allServicesAvailable = dataStoreAvailable && evaluationServiceAvailable;
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
      logger.error(String.format("API prerequisite services are not available.  Availability: data-store: %s, evaluation-service: %s",
              dataStoreAvailable, evaluationServiceAvailable));
      return false;
    }

    return true;
  }

  /**
   * Tags each Report Definition configured to be used by the API.
   * These are configured in api.report-defs, typically loaded
   * on the Evaluation/CQF Service.  Here we tag them so that they
   * can be found in ReportDefinitionController.getMeasures and yes
   * I know there is surely a better way.
   */
  private void tagReportDefinitions() {
    FhirDataProvider evaluationService = new FhirDataProvider(config.getEvaluationService());
    logger.info("Tagging Report Definitions with System '{}' and Code '{}'",
            Constants.MainSystem,
            Constants.ReportDefinitionTag);

    config.getReportDefs().getBundles().parallelStream().forEach(
            bundleConfig -> {

              logger.info("Pulling Report Definition - {}",
                      bundleConfig.getBundleId());

              Bundle measureBundle = evaluationService.getBundleById(bundleConfig.getBundleId());

              if (measureBundle.getMeta().getTag(Constants.MainSystem, Constants.ReportDefinitionTag) == null) {
                logger.info("Tagging and Saving Report Definition - {}",
                        bundleConfig.getBundleId());
                measureBundle.getMeta().addTag(Constants.MainSystem, Constants.ReportDefinitionTag, null);
                evaluationService.updateResource(measureBundle);
              } else {
                logger.info("Report Definition - {} - already tagged",
                        bundleConfig.getBundleId());
              }
            }
    );
  }

  private void loadSearchParameters() {
    // TODO - update these search params in repo (if necessary)
    try {
      FhirContext ctx = FhirContextProvider.getFhirContext();
      IParser xmlParser = ctx.newXmlParser();
      logger.info(String.format("Resources count: %d", resources.length));
      for (final Resource res : resources) {
        try (InputStream inputStream = res.getInputStream()) {
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
        logger.error(String.format("Error %s in socket-timeout %s ", ex.getMessage(), config.getSocketTimeout()));
      }
    }
    return socketTimeout;
  }

  public void init() {
    this.ctx.getRestfulClientFactory().setSocketTimeout(getSocketTimout());

    Optional<ApiReportDefsBundleConfig> measureReportAggregator = config.getReportDefs().getBundles().stream().filter(bundleConfig -> StringUtils.isEmpty(bundleConfig.getReportAggregator())).findFirst();
    if (StringUtils.isEmpty(config.getReportAggregator()) && measureReportAggregator.isPresent()) {
      String msg = "Not all measures have aggregators configured and there is no default aggregator in the configuration file.";
      logger.error(msg);
      throw new IllegalStateException(msg);
    }
    /*
    Optional<ApiReportDefsUrlConfig> measureReportAggregator = config.getReportDefs().getUrls().stream().filter(urlConfig -> StringUtils.isEmpty(urlConfig.getReportAggregator())).findFirst();
    if (StringUtils.isEmpty(config.getReportAggregator()) && !measureReportAggregator.isEmpty()) {
      String msg = "Not all measures have aggregators configured and there is no default aggregator in the configuration file.";
      logger.error(msg);
      throw new IllegalStateException(msg);
    }

     */

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

    // ALM 28Jun2023 - for now I have commented this out because it
    // pulls an OLD measure from nshslink.org and loads that onto the CQF and DataStore servers
    // So even though when spinning up a new CQF I was loading the correct Measure, this was
    // overwriting it.
    //this.loadMeasureDefinitions();
    //this.loadSearchParameters();
    this.tagReportDefinitions();
  }

}
