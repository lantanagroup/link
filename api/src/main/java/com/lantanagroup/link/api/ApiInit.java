package com.lantanagroup.link.api;

import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiReportDefsUrlConfig;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.db.MongoService;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

public class ApiInit {
  private static final Logger logger = LoggerFactory.getLogger(ApiInit.class);

  @Autowired
  private ApiConfig config;

  @Autowired
  private QueryConfig queryConfig;

  @Autowired
  private USCoreConfig usCoreConfig;

  @Autowired
  private MongoService mongoService;

  private boolean checkPrerequisites() {
    logger.info("Checking that API prerequisite services are available. maxRetry: {}, retryWait: {}", config.getMaxRetry(), config.getRetryWait());

    boolean allServicesAvailable = false;
    boolean terminologyServiceAvailable = false;
    boolean evaluationServiceAvailable = false;

    for (int retry = 0; config.getMaxRetry() == null || retry <= config.getMaxRetry(); retry++) {
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
      allServicesAvailable = terminologyServiceAvailable && evaluationServiceAvailable;
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
      logger.error("API prerequisite services are not available. Availability: terminology-service: {}, evaluation-service: {}",
              terminologyServiceAvailable, evaluationServiceAvailable);
      return false;
    }

    return true;
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
    FhirContextProvider.getFhirContext().getRestfulClientFactory().setSocketTimeout(getSocketTimout());

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
      FhirContextProvider.getFhirContext().getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
      logger.info("Setting client to never query for metadata");
    }

    // check that prerequisite services are available
    if (!this.checkPrerequisites()) {
      throw new IllegalStateException("Prerequisite services check failed. Cannot continue API initialization.");
    }
  }
}
