package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientsQueriedMetric {
  private long total;
  private long[] history;
}
