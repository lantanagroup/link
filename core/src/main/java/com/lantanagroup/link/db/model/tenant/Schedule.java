package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Schedule {
  private String queryPatientListCron;
  private String dataRetentionCheckCron;
  private List<GenerateReport> generateAndSubmitReports;
}
