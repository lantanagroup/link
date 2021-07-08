package com.lantanagroup.link.model;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.HashMap;

@Getter @Setter
public class ReportContext {
  public ReportContext(IGenericClient fhirStoreClient, FhirContext fhirContext) {
    this.setFhirStoreClient(fhirStoreClient);
    this.setFhirContext(fhirContext);
  }

  private IGenericClient fhirStoreClient;
  private FhirContext fhirContext;
  private String measureId;
  private Bundle reportDefBundle;
  private String reportId;
  private MeasureReport measureReport;
  private HashMap<String, Object> additional = new HashMap<>();
}
