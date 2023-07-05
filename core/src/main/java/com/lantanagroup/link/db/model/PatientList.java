package com.lantanagroup.link.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientList {
  private UUID id = UUID.randomUUID();
  private String measureId;
  private String periodStart;
  private String periodEnd;
  private List<PatientId> patients = new ArrayList<>();
  private Date lastUpdated;

  public void merge(PatientList source) {
    this.lastUpdated = source.lastUpdated;

    for (PatientId sourcePatientId : source.getPatients()) {
      if (!this.patients.contains(sourcePatientId)) {
        this.patients.add(sourcePatientId);
      }
    }
  }
}
