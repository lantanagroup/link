package com.lantanagroup.link.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientList {
  private String id = (new ObjectId()).toString();
  private String periodStart;
  private String periodEnd;
  private String measureId;
  private Date lastUpdated;
  private List<PatientId> patients = new ArrayList<>();

  public void merge(PatientList source) {
    this.lastUpdated = source.lastUpdated;

    for (PatientId sourcePatientId : source.getPatients()) {
      if (!this.patients.contains(sourcePatientId)) {
        this.patients.add(sourcePatientId);
      }
    }
  }
}
