package com.lantanagroup.link.config.datagovernance;

import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "data-governance")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class DataGovernanceConfig {
  /**
   * <strong>data-governance.retention-period.census-list-retention</strong><br>Contains the retention periods for the census list.
   */
  private String censusListRetention;

  /**
   * <strong>data-governance.retention-period.patient-data-retention</strong><br>Contains the retention periods for the patient data.
   */
  private String patientDataRetention;

  /**
   * <strong>data-governance.retention-period.report-retention</strong><br>Contains the retention periods for the report.
   */
  private String reportRetention;
}