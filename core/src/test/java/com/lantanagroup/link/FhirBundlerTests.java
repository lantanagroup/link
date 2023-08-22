package com.lantanagroup.link;

import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Aggregate;
import com.lantanagroup.link.db.model.PatientList;
import com.lantanagroup.link.db.model.PatientMeasureReport;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.db.model.tenant.Bundling;
import com.lantanagroup.link.db.model.tenant.Tenant;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.codesystems.MeasurePopulation;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class FhirBundlerTests {
  private int patientMeasureReportCount = 0;

  private <T extends Resource> T deserializeResource(String resourcePath, Class<T> clazz) {
    InputStream measureStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
    return FhirContextProvider.getFhirContext().newJsonParser().parseResource(clazz, measureStream);
  }

  private PatientMeasureReport getPatientMeasureReport(int populationCount) {
    this.patientMeasureReportCount++;

    PatientMeasureReport pmr = new PatientMeasureReport();
    MeasureReport mr = new MeasureReport();
    Patient patient = new Patient();

    mr.setId("test-mr" + this.patientMeasureReportCount);
    mr.setType(MeasureReport.MeasureReportType.INDIVIDUAL);
    mr.setSubject(new Reference("Patient/test-patient" + this.patientMeasureReportCount));
    mr.addGroup().addPopulation()
            .setCode(new CodeableConcept(new Coding().setCode(MeasurePopulation.INITIALPOPULATION.toCode())))
            .setCount(populationCount);

    patient.setId("test-patient" + this.patientMeasureReportCount);
    patient.addName().setFamily("Patient" + this.patientMeasureReportCount).addGiven("Test" + this.patientMeasureReportCount);

    mr.addEvaluatedResource(new Reference("Patient/test-patient" + this.patientMeasureReportCount));
    mr.addContained(patient);
    pmr.setMeasureReport(mr);

    return pmr;
  }

  @Test
  public void testBundle() {
    // Use legacy behavior of reifying/promoting line-level resources
    Bundling bundlingConfig = new Bundling();
    bundlingConfig.setIncludeCensuses(true);
    bundlingConfig.setPromoteLineLevelResources(true);
    bundlingConfig.setNpi("test-org-npi");

    Tenant tenant = new Tenant();
    tenant.setBundling(bundlingConfig);

    TenantService tenantService = mock(TenantService.class);
    MeasureReport masterMeasureReport = this.deserializeResource("master-mr1.json", MeasureReport.class);
    Aggregate aggregate = new Aggregate();
    aggregate.setReport(masterMeasureReport);

    FhirBundler bundler = new FhirBundler(null, tenantService, new ApiConfig());

    Report report = new Report();

    List<PatientList> patientLists = new ArrayList<>();
    PatientList patientList = new PatientList();
    patientList.setId(UUID.randomUUID());
    patientLists.add(patientList);

    when(tenantService.getConfig()).thenReturn(tenant);
    when(tenantService.getPatientLists(any())).thenReturn(patientLists);

    List<PatientMeasureReport> pmrs = List.of(this.getPatientMeasureReport(0), this.getPatientMeasureReport(1), this.getPatientMeasureReport(2));
    when(tenantService.getPatientMeasureReports(any())).thenReturn(pmrs);
    when(tenantService.getPatientMeasureReports(any(), any())).thenReturn(pmrs);

    // Generate the bundle
    Bundle bundle = bundler.generateBundle(List.of(aggregate), report);

    Assert.assertNotNull(bundle);
    Assert.assertEquals(8, bundle.getEntry().size());

    // Organization tests
    Assert.assertEquals(ResourceType.Organization, bundle.getEntry().get(0).getResource().getResourceType());
    Assert.assertEquals(ResourceType.Device, bundle.getEntry().get(1).getResource().getResourceType());
    Assert.assertEquals(ResourceType.List, bundle.getEntry().get(2).getResource().getResourceType());

    // Aggregate measure report
    Assert.assertEquals(ResourceType.MeasureReport, bundle.getEntry().get(3).getResource().getResourceType());

    // First patient in IP measure report and promoted resources
    Assert.assertEquals(ResourceType.MeasureReport, bundle.getEntry().get(4).getResource().getResourceType());
    Assert.assertTrue(((MeasureReport) bundle.getEntry().get(4).getResource()).getContained().isEmpty());
    Assert.assertEquals(ResourceType.Patient, bundle.getEntry().get(5).getResource().getResourceType());
    Assert.assertEquals("test-patient2", bundle.getEntry().get(5).getResource().getIdElement().getIdPart());

    // Second patient in IP measure report and promoted resources
    Assert.assertEquals(ResourceType.MeasureReport, bundle.getEntry().get(6).getResource().getResourceType());
    Assert.assertTrue(((MeasureReport) bundle.getEntry().get(6).getResource()).getContained().isEmpty());
    Assert.assertEquals(ResourceType.Patient, bundle.getEntry().get(7).getResource().getResourceType());
    Assert.assertEquals("test-patient3", bundle.getEntry().get(7).getResource().getIdElement().getIdPart());

    // Ensure test-patient1 is excluded
    boolean testPatient1Found = bundle.getEntry().stream().anyMatch(e -> {
      if (e.getResource() instanceof MeasureReport) {
        MeasureReport mr = (MeasureReport) e.getResource();
        return mr.getSubject().hasReference() && mr.getSubject().getReference().equalsIgnoreCase("Patient/test-patient1");
      } else if (e.getResource() instanceof Patient) {
        Patient p = (Patient) e.getResource();
        return p.getIdElement().getIdPart().equalsIgnoreCase("test-patient1");
      }
      return false;
    });
    Assert.assertFalse(testPatient1Found);

    MeasureReport indMeasureReport = (MeasureReport) bundle.getEntry().get(4).getResource();
    Assert.assertEquals(MeasureReport.MeasureReportType.INDIVIDUAL, indMeasureReport.getType());
  }
}
