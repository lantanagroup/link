package com.lantanagroup.link.db.model;

import com.lantanagroup.link.ReportingPeriodMethods;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@SuppressWarnings("unused")
@Getter
@Setter
public class TenantGenerateReportConfig {
  private String cron;
  private List<String> measureIds;
  private ReportingPeriodMethods reportingPeriodMethod;

  private Boolean regenerateIfExists = false;
}
