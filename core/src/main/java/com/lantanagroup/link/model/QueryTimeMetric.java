package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryTimeMetric {
  private double average;
  private double[] history;
}
