package com.lantanagroup.link.model;

import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.auth.LinkCredentials;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ReportContext {
  private FhirDataProvider fhirProvider;

  public ReportContext(FhirDataProvider fhirProvider) {
    this.setFhirProvider(fhirProvider);
  }

  // TODO: Create a new measure-specific context class; move measure-specific fields into that class
  //       Here, maintain a map of measure-specific contexts keyed by report definition identifier
  private String measureId;
  private Bundle reportDefBundle;
  private String reportId;
  private String inventoryId;  // TODO: Remove (see usage in ReportController.generateReport)
  private MeasureReport measureReport;
  private List<QueryResponse> patientData = new ArrayList<>();  // TODO: Remove? Currently unused
  private Measure measure;
  private List<ListResource> patientCensusLists = new ArrayList<>();
  private List<PatientOfInterestModel> patientsOfInterest = new ArrayList<>();
  private List<ConceptMap> conceptMaps = new ArrayList();  // TODO: Remove? Currently unused
  HttpServletRequest request;
  LinkCredentials user;
}
