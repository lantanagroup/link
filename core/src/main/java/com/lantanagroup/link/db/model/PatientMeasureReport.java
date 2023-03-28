package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.hl7.fhir.r4.model.MeasureReport;

@Getter
@Setter
public class PatientMeasureReport {
  private String id = (new ObjectId()).toString();
  private String patientId;
  private String measureId;
  private String periodStart;
  private String periodEnd;
  private MeasureReport measureReport;
}
