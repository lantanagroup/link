package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvaluationMetric {
  private double average;
  private double[] history;
}
