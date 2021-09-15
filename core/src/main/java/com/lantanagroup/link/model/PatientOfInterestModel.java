package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PatientOfInterestModel {
  private String reference;
  private String identifier;

  public PatientOfInterestModel() {

  }

  public PatientOfInterestModel(String reference, String identifier) {
    this.reference = reference;
    this.identifier = identifier;
  }

  @Override
  public String toString() {
    return this.reference != null ?
            this.reference :
            this.identifier;
  }
}
