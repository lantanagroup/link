package com.lantanagroup.link;

import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.PatientData;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.time.StopwatchManager;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MeasureEvaluatorTests {
  private TenantService tenantService;
  private StopwatchManager stopwatchManager = new StopwatchManager();
  private Bundle measureBundle = this.getMeasureBundle();

  @Before
  public void init() {
    this.tenantService = mock(TenantService.class);
  }

  public Measure getMeasure() {
    return this.measureBundle.getEntry().stream()
            .filter(e -> e.getResource().getResourceType() == ResourceType.Measure)
            .map(e -> (Measure) e.getResource())
            .findFirst()
            .orElse(null);
  }

  private Bundle getPatientBundle() {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream("pre-eval-patient-data-bundle.json");
    return FhirContextProvider.getFhirContext().newJsonParser().parseResource(Bundle.class, is);
  }

  private Bundle getMeasureBundle() {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream("adult-patients-measure-bundle.json");
    return FhirContextProvider.getFhirContext().newJsonParser().parseResource(Bundle.class, is);
  }

  @Test
  public void measureEvalTest() {
    List<PatientData> patientData = this.getPatientBundle().getEntry().stream().map(e -> {
      PatientData newPatientData = new PatientData();
      newPatientData.setResource(e.getResource());
      return newPatientData;
    }).collect(Collectors.toList());

    when(this.tenantService.findPatientData("Hypo-Patient1-REQ")).thenReturn(patientData);

    ReportCriteria criteria = new ReportCriteria("test-measure", "2022-04-01T00:00:00.000Z", "2022-04-30T23:59:59.000Z");
    String masterReportId = ReportIdHelper.getMasterIdentifierValue(criteria);

    PatientOfInterestModel poi = new PatientOfInterestModel("Patient/Hypo-Patient1-REQ", null);
    poi.setId("Hypo-Patient1-REQ");

    ReportContext context = new ReportContext();
    context.setMasterIdentifierValue(masterReportId);
    ReportContext.MeasureContext measureContext = new ReportContext.MeasureContext();
    measureContext.setMeasure(this.getMeasure());
    measureContext.setReportDefBundle(this.measureBundle);


    MeasureReport measureReport = MeasureEvaluator.generateMeasureReport(
            this.tenantService,
            this.stopwatchManager,
            criteria,
            context,
            measureContext,
            new ApiConfig(),
            poi);
  }
}
