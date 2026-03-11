package com.lantanagroup.link.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ReportSummary {
  private String reportId;
  private String periodStart;
  private String periodEnd;
}
