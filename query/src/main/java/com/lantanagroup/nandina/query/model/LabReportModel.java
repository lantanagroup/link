package com.lantanagroup.nandina.query.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LabReportModel {
  private String patientId;
  private String order;
  private String date;
  private String result;
}
