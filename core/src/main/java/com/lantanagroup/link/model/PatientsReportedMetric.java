package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientsReportedMetric {
  private long total;
  private long[] history;
}
