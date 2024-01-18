package com.lantanagroup.link.api;

import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.tenant.FhirQuery;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.validation.Validator;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Automatically detected when the Spring Boot application starts up. Primary purpose is to validate configurations.
 */
public class ApiInit {
  private static final Logger logger = LoggerFactory.getLogger(ApiInit.class);

  @Autowired
  private ApiConfig config;

  @Autowired
  private SharedService sharedService;

  @Autowired
  private Validator validator;

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
        logger.warn("Thread was interrupted, while sleeping, while waiting for other services to come online");
        Thread.currentThread().interrupt();
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
        logger.error("Error {} in socket-timeout {} ", ex.getMessage(), config.getSocketTimeout());
      }
    }
    return socketTimeout;
  }

  private void initDatabases(List<Tenant> tenants) {
    for (Tenant tenant : tenants) {
      TenantService tenantService = TenantService.create(this.sharedService, tenant.getId());
      assert tenantService != null : "Could not create tenant service for tenant " + tenant.getId();
      tenantService.initDatabase();
    }
  }

  public void init() {
    FhirContextProvider.getFhirContext().getRestfulClientFactory().setSocketTimeout(getSocketTimout());
    if (this.config.getValidateFhirServer() != null && !this.config.getValidateFhirServer()) {
      FhirContextProvider.getFhirContext().getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
      logger.info("Setting client to never query for metadata");
    }

    if (!this.config.isApplySchemas()) {
      logger.warn("Not configured to apply schemas to database. Skipping shared database schema init.");
    } else {
      this.sharedService.initDatabase();
    }

    this.validator.init();
    List<Tenant> tenants = this.sharedService.getTenantConfigs();

    for (Tenant tenant : tenants) {
      FhirQuery fhirQuery = tenant.getFhirQuery();
      if (fhirQuery == null || StringUtils.isEmpty(fhirQuery.getFhirServerBase())) {
        logger.error("Tenant {} does not specify FHIR server base", tenant.getId());
      } else if (this.config.isRequireHttps() && !fhirQuery.getFhirServerBase().toLowerCase().startsWith("https://")) {
        logger.error("HTTPS is required, but tenant {} does not have an HTTPS FHIR server base", tenant.getId());
      }
    }

    if (!this.config.isApplySchemas()) {
      logger.warn("Not configured to apply schemas to database. Skipping tenant database schema init.");
    } else {
      this.initDatabases(tenants);
    }

    if (this.config.getSkipInit()) {
      logger.info("Skipping API initialization processes");
      return;
    }

    // check that prerequisite services are available
    if (!this.checkPrerequisites()) {
      throw new IllegalStateException("Prerequisite services check failed. Cannot continue API initialization.");
    }
  }
}
