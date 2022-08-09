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

  private String measureId;
  private Bundle reportDefBundle;
  private String reportId;
  private String inventoryId;
  private MeasureReport measureReport;
  private List<QueryResponse> patientData = new ArrayList<>();
  private Measure measure;
  private List<ListResource> patientCensusLists = new ArrayList<>();
  private List<PatientOfInterestModel> patientsOfInterest = new ArrayList<>();
  private List<ConceptMap> conceptMaps = new ArrayList();
  HttpServletRequest request;
  LinkCredentials user;
}
