package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.MeasureReport;

@Getter
@Setter
public class PatientMeasureReport {
  private String id;
  private String reportId;
  private String measureId;
  private String patientId;
  private MeasureReport measureReport;
}
