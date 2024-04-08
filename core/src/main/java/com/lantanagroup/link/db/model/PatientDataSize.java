package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientDataSize {
  private String patientId;
  private String resourceType;
  private double SizeKb;
}