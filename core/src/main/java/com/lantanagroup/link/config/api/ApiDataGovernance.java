package com.lantanagroup.link.config.api;


import com.lantanagroup.link.config.YamlPropertySourceFactory;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

@Data
@Configuration
@ConfigurationProperties(prefix = "api.data-governance")
@Validated
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class ApiDataGovernance {
  /**
   * <strong>api.data-governance.retention-period</strong><br>Contains the retention periods for the census list, patient data, and the report.
   */
  @Getter
  private RetentionPeriod retentionPeriod = new RetentionPeriod();
}

class RetentionPeriod {
  /**
   * <strong>api.data-governance.retention-period.census-list-retention</strong><br>Contains the retention period for the census list.
   */
  @Getter
  private String censusListRetention;

  /**
   * <strong>api.data-governance.retention-period.patient-data-retention</strong><br>Contains the retention period for the patient data.
   */
  @Getter
  private String patientDataRetention;

  /**
   * <strong>api.data-governance.retention-period.report-retention</strong><br>Contains the retention period for the report.
   */
  @Getter
  private String reportRetention;
}
