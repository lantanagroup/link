package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientMeasureReportSize {
  private String reportId;
  private String measureId;
  private String patientId;
  private double SizeKb;
}