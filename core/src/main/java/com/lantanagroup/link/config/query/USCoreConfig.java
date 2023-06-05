package com.lantanagroup.link.config.query;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import java.time.Period;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "uscore")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class USCoreConfig {

  /**
   * <strong>uscore.patient-resource-types</strong><br>The list of resource types supported by the configured EHR's FHIR API that are specific to a patient (support the 'patient' search parameter)
   */
  private List<String> patientResourceTypes;

  /**
   * <strong>query.fhir-server-base</strong><br>The base URL of the FHIR server that should be queried
   */
  @NotBlank
  @URL
  private String fhirServerBase;

  /**
   * <strong>uscore.other-resource-types</strong><br>The list of resource types supported by the configured EHR's FHIR API that are NOT specific to a patient. These are queried via references from patient resources.
   */
  private List<USCoreOtherResourceTypeConfig> otherResourceTypes;

  /**
   * <strong>uscore.parallel-patients</strong><br>The number of patients to query for at a single time.
   */
  private int parallelPatients = 10;

  /**
   * <strong>uscore.query-parameters</strong><br>Query parameters for individual measures.
   */
  private HashMap<String, List<USCoreQueryParametersResourceConfig>> queryParameters;

  /**
   * <strong>uscore.lookback-period</strong><br>For date-constrained searches, the length of time to search before the beginning of the reporting period
   */
  private Period lookbackPeriod;

  /**
   * <strong>uscore.encounter-based</strong><br>Whether to exit immediately from the query phase if no encounters are found
   */
  private boolean encounterBased = true;
}

