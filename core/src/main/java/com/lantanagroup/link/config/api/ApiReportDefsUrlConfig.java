package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@Validated
public class ApiReportDefsUrlConfig {
  /**
   * <strong>api.report-defs.urls.bundle-id</strong><br>ID to be applied to the bundle before storage.
   */
  @NotBlank
  private String bundleId;

  /**
   * <strong>api.report-defs.urls.url</strong><br>URL used to retrieve the measure definition bundle.
   */
  @NotBlank
  private String url;

  /**
   * <strong>api.report-defs.urls.census-identifier</strong><br>Identifier to be applied to the patient list before storage.
   */
  private String censusIdentifier;

  /**
   * <strong>api.report-defs.urls.patient-list-id</strong><br>ID used to retrieve the patient list from the query server.
   */
  private String patientListId;

  /**
   * <strong>api.report-defs.urls.report-aggregator</strong><br>Aggregator used to aggregate for that measure.
   */
  private String reportAggregator;
}
