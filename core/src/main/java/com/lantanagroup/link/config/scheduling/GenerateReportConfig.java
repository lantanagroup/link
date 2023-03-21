package com.lantanagroup.link.config.scheduling;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GenerateReportConfig {
  private String cron;
  private List<String> measureIds;
  private ReportingPeriodMethods reportingPeriodMethod;

  private Boolean regenerateIfExists = false;
}
