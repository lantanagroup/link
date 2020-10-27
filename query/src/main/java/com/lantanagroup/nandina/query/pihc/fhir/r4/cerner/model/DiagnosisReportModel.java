package com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DiagnosisReportModel {
  private String patientId;
  private String code;
}
