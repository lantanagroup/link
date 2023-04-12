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
import java.util.Collections;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "uscore")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class USCoreConfig {
  /**
   * <strong>query.fhir-server-base</strong><br>The base URL of the FHIR server that should be queried
   */
  @NotBlank
  @URL
  private String fhirServerBase;

  /**
   * <strong>uscore.parallel-patients</strong><br>The number of patients to query for at a single time.
   */
  private int parallelPatients = 10;

  /**
   * <strong>uscore.query-plans</strong><br>Query plans keyed by measure bundle ID or multi-measure package ID.
   */
  private Map<String, QueryPlan> queryPlans = Collections.emptyMap();
}
