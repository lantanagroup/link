package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

@Getter @Setter
public class ReportCriteria {
  public ReportCriteria(String reportDefId, String periodStart, String periodEnd) {
    this.setReportDefId(reportDefId);
    this.setPeriodStart(periodStart);
    this.setPeriodEnd(periodEnd);
  }

  String reportDefId;
  String periodStart;
  String periodEnd;
  String measureId;
  HashMap<String, String> additional = new HashMap<>();
}
