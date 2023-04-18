package com.lantanagroup.link.db.model.tenant;

import com.lantanagroup.link.ReportingPeriodMethods;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@SuppressWarnings("unused")
@Getter
@Setter
public class GenerateReport {
  /**
   * The CRON-formatted schedule to use for this report's generation and submission
   */
  private String cron;

  /**
   * The IDs of the measures to generate the report for
   */
  private List<String> measureIds;

  /**
   * The method that should be used to calculate the reporting period for the report.
   */
  private ReportingPeriodMethods reportingPeriodMethod;

  /**
   * If a report already exists for the calculated reporting period and measures, indicates whether or not the
   * report generation process should continue and overwrite the already existing report.
   */
  private Boolean regenerateIfExists = false;
}
