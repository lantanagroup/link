package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;

@Getter
@Setter
public class QueryResponse {
  String patientId;
  Bundle bundle;

  public QueryResponse(String patientId, Bundle bundle) {
    this.patientId = patientId;
    this.bundle = bundle;
  }

  public QueryResponse() {

  }
}
