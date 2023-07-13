package com.lantanagroup.link;

import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.*;
import com.lantanagroup.link.db.model.tenant.Bundling;
import com.lantanagroup.link.db.model.tenant.Tenant;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class FhirBundlerTests {
  private <T extends Resource> T deserializeResource(String resourcePath, Class<T> clazz) {
    InputStream measureStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
    return FhirContextProvider.getFhirContext().newJsonParser().parseResource(clazz, measureStream);
  }

  @Test
  public void testBundle() {
    // Use legacy behavior of reifying/promoting line-level resources
    Bundling bundlingConfig = new Bundling();
    bundlingConfig.setIncludeCensuses(true);
    bundlingConfig.setPromoteLineLevelResources(false);
    bundlingConfig.setNpi("test-org-npi");

    Tenant tenant = new Tenant();
    tenant.setBundling(bundlingConfig);

    TenantService tenantService = mock(TenantService.class);
    MeasureReport masterMeasureReport = this.deserializeResource("master-mr1.json", MeasureReport.class);
    Aggregate aggregate = new Aggregate();
    aggregate.setReport(masterMeasureReport);

    FhirBundler bundler = new FhirBundler(null, tenantService);

    Report report = new Report();

    List<PatientList> patientLists = new ArrayList<>();
    PatientList patientList = new PatientList();
    patientList.setId(UUID.randomUUID());
    patientLists.add(patientList);

    when(tenantService.getConfig()).thenReturn(tenant);
    when(tenantService.getPatientLists(any())).thenReturn(patientLists);

    PatientMeasureReport pmr1 = new PatientMeasureReport();
    pmr1.setMeasureReport(new MeasureReport());
    pmr1.getMeasureReport().setId("test-mr");
    pmr1.getMeasureReport().setType(MeasureReport.MeasureReportType.INDIVIDUAL);
    when(tenantService.getPatientMeasureReports(any())).thenReturn(List.of(pmr1));
    when(tenantService.getPatientMeasureReports(any(), any())).thenReturn(List.of(pmr1));

    // Generate the bundle
    Bundle bundle = bundler.generateBundle(List.of(aggregate), report);

    Assert.assertNotNull(bundle);
    Assert.assertEquals(4, bundle.getEntry().size());

    // Organization tests
    Assert.assertEquals(ResourceType.Organization, bundle.getEntry().get(0).getResource().getResourceType());
    Assert.assertEquals(ResourceType.List, bundle.getEntry().get(1).getResource().getResourceType());
    Assert.assertEquals(ResourceType.MeasureReport, bundle.getEntry().get(2).getResource().getResourceType());
    Assert.assertEquals(ResourceType.MeasureReport, bundle.getEntry().get(3).getResource().getResourceType());

    MeasureReport indMeasureReport = (MeasureReport) bundle.getEntry().get(3).getResource();
    Assert.assertEquals(MeasureReport.MeasureReportType.INDIVIDUAL, indMeasureReport.getType());
  }
}
