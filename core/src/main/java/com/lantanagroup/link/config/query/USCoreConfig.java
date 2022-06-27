package com.lantanagroup.link.config.query;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "uscore")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class USCoreConfig {

  /**
   * <strong>query.patient-resource-types</strong><br>The list of resource types supported by the configured EHR's FHIR API that are specific to a patient (support the 'patient' search parameter)
   */
  private List<String> patientResourceTypes;

  /**
   * <strong>query.other-resource-types</strong><br>The list of resource types supported by the configured EHR's FHIR API that are NOT specific to a patient. These are queried via references from patient resources.
   */
  private List<String> otherResourceTypes;

  /**
   * <strong>query.parallel-patients</strong><br>The number of patients to query for at a single time.
   */
  private int parallelPatients = 10;
}
