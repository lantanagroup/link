package com.lantanagroup.link.config.query;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

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
  @Getter
  private List<String> patientResourceTypes;

  /**
   * <strong>uscore.other-resource-types</strong><br>The list of resource types supported by the configured EHR's FHIR API that are NOT specific to a patient. These are queried via references from patient resources.
   */
  @Getter
  private List<String> otherResourceTypes;

  /**
   * <strong>uscore.parallel-patients</strong><br>The number of patients to query for at a single time.
   */
  @Getter
  private int parallelPatients = 10;

  /**
   * <strong>uscore.query-parameters</strong><br>Query parameters for individual measures.
   */
  @Getter
  private HashMap<String, List<USCoreQueryParametersResourceConfig>> queryParameters;

}

