package com.lantanagroup.link.config.DataGovernance;

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
public class dataGovernanceConfig {
  /**
   * <strong>data-governance.retention-period</strong><br>Contains the retention periods for the census list, patient data, and the report.
   */
  @Getter
  private RetentionPeriod retentionPeriod = new RetentionPeriod();
}

class RetentionPeriod {
  /**
   * <strong>data-governance.retention-period.census-list-retention</strong><br>Contains the retention periods for the census list.
   */
  @Getter
  private String censusListRetention;

  /**
   * <strong>data-governance.retention-period.patient-data-retention</strong><br>Contains the retention periods for the patient data.
   */
  @Getter
  private String patientDataRetention;

  /**
   * <strong>data-governance.retention-period.report-retention</strong><br>Contains the retention periods for the report.
   */
  @Getter
  private String reportRetention;
}