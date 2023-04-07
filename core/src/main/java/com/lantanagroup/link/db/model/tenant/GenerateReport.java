package com.lantanagroup.link.db.model.tenant;

import com.lantanagroup.link.ReportingPeriodMethods;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@SuppressWarnings("unused")
@Getter
@Setter
public class GenerateReport {
  private String cron;
  private List<String> measureIds;
  private ReportingPeriodMethods reportingPeriodMethod;

  private Boolean regenerateIfExists = false;
}
