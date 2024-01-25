package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientListSearchResponse {
  private String id;
  private String measureId;
  private String periodStart;
  private String periodEnd;
  private Integer totalPatients;
}
