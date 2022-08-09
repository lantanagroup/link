package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiReportDefsUrlConfig {
  /**
   * <string>api.report-defs.urls.url</string><br>URL used to retrieve the measure definition bundle.
   */
  private String url;

  /**
   * <string>api.report-defs.urls.census-identifier</string><br>Identifier to be applied to the patient list before storage.
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
