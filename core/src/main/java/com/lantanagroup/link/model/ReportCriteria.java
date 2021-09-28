package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

@Getter
@Setter
public class ReportCriteria {

  String reportDefIdentifier;
  String reportDefId;
  String periodStart;
  String periodEnd;
  String measureId;
  HashMap<String, String> additional = new HashMap<>();

  public ReportCriteria(String reportDefIdentifier, String periodStart, String periodEnd) {
    this.setReportDefIdentifier(reportDefIdentifier);
    this.setPeriodStart(periodStart);
    this.setPeriodEnd(periodEnd);
  }
}
