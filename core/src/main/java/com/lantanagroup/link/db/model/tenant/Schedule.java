package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Schedule {
  /**
   * The CRON-formatted schedule to use querying patient list information from the EHR.
   * The equivalent of manually calling /api/{tenantId}/poi/$query-list
   */
  private String queryPatientListCron;

  /**
   * The CRON-formatted schedule to use for data retention checks (removing data that is outside the
   * tenant's configured retention period).
   */
  private String dataRetentionCheckCron;

  /**
   * Configuration for when to automatically generate and submit reports.
   */
  private List<GenerateReport> generateAndSubmitReports = new ArrayList<>();

  private String bulkDataCron;

  private String bulkDataFollowUpCron;
}
