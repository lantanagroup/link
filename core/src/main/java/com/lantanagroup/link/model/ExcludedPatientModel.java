package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.CodeableConcept;

@Getter @Setter
public class ExcludedPatientModel {
  private String patientId;
  private CodeableConcept reason;
}
