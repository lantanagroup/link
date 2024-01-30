package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MetricsReportResponse {
  private QueryTimeMetric queryTime;
  private PatientsQueriedMetric patientsQueried;
  private PatientsReportedMetric patientsReported;
  private ValidationMetric validation;
  private ValidationMetric validationIssues;
  private EvaluationMetric evaluation;
}

