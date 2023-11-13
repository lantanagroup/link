package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MetricsReportResponse {
  private QueryTimeMetric QueryTime;
  private PatientsQueriedMetric PatientsQueried;
  private PatientsReportedMetric PatientsReported;
  private ValidationMetric Validation;
  private EvaluationMetric Evaluation;
}

