package com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MedReportModel {
  private String patientId;
  private String name;
  private String code;
  private String dose;
  private String route;
  private String start;
  private String end;
}
