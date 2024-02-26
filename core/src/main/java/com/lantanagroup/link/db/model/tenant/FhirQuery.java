package com.lantanagroup.link.db.model.tenant;

import com.lantanagroup.link.db.model.tenant.auth.*;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;

@Getter
@Setter
public class FhirQuery {
  @NotNull
  private String fhirServerBase;

  /**
   * The class that should be used (if any) to authenticate queries to the EHR's FHIR server.
   */
  private String authClass;

  /**
   * The number of patients to query for in parallel using separate threads. Should not be greater than the number of
   * cores available to the installation.
   */
  private int parallelPatients = 10;

  /**
   * Configuration used by BasicAuth implementation
   */
  private BasicAuth basicAuth;

  /**
   * Configuration used by BasicAuthAndApiKeyHeader implementation
   */
  private BasicAuthAndApiKey basicAuthAndApiKey;

  /**
   * Configuration used by TokenAuth implementation
   */
  private TokenAuth tokenAuth;

  /**
   * Configuration used by AzureAuth implementation
   */
  private AzureAuth azureAuth;

  /**
   * Configuration used by EpicAuth implementation
   */
  private EpicAuth epicAuth;

  /**
   * Configuration used by CernerAuth implementation
   */
  private CernerAuth cernerAuth;

  private Map<String, String> queryPlanUrls = Collections.emptyMap();
  private Map<String, QueryPlan> queryPlans = Collections.emptyMap();
}
