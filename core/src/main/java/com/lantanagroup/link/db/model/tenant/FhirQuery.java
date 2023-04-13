package com.lantanagroup.link.db.model.tenant;

import com.lantanagroup.link.db.model.tenant.auth.*;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class FhirQuery {
  @NotNull
  private String fhirServerBase;

  /**
   * The class that should be used (if any) to authenticate queries to the specified <strong>query.fhir-server-base</strong>.
   */
  private String authClass;


  /**
   * The list of resource types supported by the configured EHR's FHIR API that are specific to a patient (support the 'patient' search parameter)
   */
  private List<String> patientResourceTypes;

  /**
   * The list of resource types supported by the configured EHR's FHIR API that are NOT specific to a patient. These are queried via references from patient resources.
   */
  private List<FhirQueryOtherResourceType> otherResourceTypes;

  /**
   * The number of patients to query for at a single time.
   */
  private int parallelPatients = 10;

  /**
   * Query parameters for individual measures.
   */
  private HashMap<String, List<FhirQueryParametersResource>> queryParameters;

  /**
   * For date-constrained searches, the length of time to search before the beginning of the reporting period
   */
  private String lookbackPeriod;

  /**
   * Whether to exit immediately from the query phase if no encounters are found
   */
  private boolean encounterBased = true;

  private BasicAuth basicAuth;

  private BasicAuthAndApiKey basicAuthAndApiKey;

  private TokenAuth tokenAuth;

  private AzureAuth azureAuth;

  private EpicAuth epicAuth;

  private CernerAuth cernerAuth;

  private Map<String, QueryPlan> queryPlans = Collections.emptyMap();
}
