package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class PatientData {
  private UUID id;
  private String patientId;
  private String resourceType;
  private String resourceId;
  private IBaseResource resource;
  private Date retrieved;

  public static Bundle asBundle(List<PatientData> patientData) {
    Bundle bundle = new Bundle();
    bundle.setEntry(patientData.stream().map(pd -> {
      Bundle.BundleEntryComponent newEntry = new Bundle.BundleEntryComponent();
      newEntry.setResource((Resource) pd.getResource());
      return newEntry;
    }).collect(Collectors.toList()));
    return bundle;
  }
}
