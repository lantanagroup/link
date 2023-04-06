package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TenantScheduleConfig {
  private String queryPatientListCron;
  private String dataRetentionCheckCron;
  private List<TenantGenerateReportConfig> generateAndSubmitReports;
}
