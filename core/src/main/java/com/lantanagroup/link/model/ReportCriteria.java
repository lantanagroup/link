package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

@Getter @Setter
public class ReportCriteria {
  public ReportCriteria(String measureIdentifier, String periodStart, String periodEnd) {
    this.setMeasureIdentifier(measureIdentifier);
    this.setPeriodStart(periodStart);
    this.setPeriodEnd(periodEnd);
  }

  String reportDefId;
  String measureIdentifier;
  String periodStart;
  String periodEnd;
  String measureId;
  HashMap<String, String> additional = new HashMap<>();
}
