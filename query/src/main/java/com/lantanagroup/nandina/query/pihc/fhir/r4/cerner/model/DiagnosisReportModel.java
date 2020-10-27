package com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.model;

public class DiagnosisReportModel {
  private String patientId;
  private String code;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getPatientId() {
    return patientId;
  }

  public void setPatientId(String patientId) {
    this.patientId = patientId;
  }
}
