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
   * <strong>data-governance.census-list-retention</strong><br>Contains the retention period for the List.
   */
  private String censusListRetention;

  /**
   * <strong>data-governance.patient-data-retention</strong><br>Contains the retention period for Patient and Bundle.
   */
  private String patientDataRetention;

  /**
   * <strong>data-governance.measure-report-retention</strong><br>Contains the retention period for MeasureReport.
   */
  private String measureReportRetention;

  /**
   * <strong>data-governance.resource-type-retention</strong><br>Contains the retention period for resource types.
   */
  private String resourceTypeRetention;

  /**
   * <strong>data-governance.other-type-retention</strong><br>Contains the retention period for other resource types.
   */
  private String otherTypeRetention;

  /**
   * <strong>data-governance.expunge-role</strong><br/>The JWT role required for a user to be able to call expunge endpoints.
   */
  private String expungeRole;

  /**
   * <strong>data-governance.expunge-chunk-size</strong><br/>The number of items to expunge at a time.  This is trying to cut down on bringing back HUGE lists of data from the Data Store which has been causing issues.
   */
  private Integer expungeChunkSize;
}