package com.lantanagroup.link;

import com.lantanagroup.link.config.bundler.BundlerConfig;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.mockito.Mockito.*;

public class FhirBundlerTests {
  private <T extends Resource> T deserializeResource(String resourcePath, Class<T> clazz) {
    InputStream measureStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
    return FhirContextProvider.getFhirContext().newJsonParser().parseResource(clazz, measureStream);
  }

  /**
   * Tests that a patient who is in the initial-population produces a bundle that includes contained resources
   * that are referenced in the patient measure report's "extension-supplementalData" extensions.
   */
  @Test
  public void testBundleReferences() {
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);

    MeasureReport masterMeasureReport = this.deserializeResource("master-mr1.json", MeasureReport.class);
    Bundle patientMeasureReports = this.deserializeResource("patient-mr-bundle1.json", Bundle.class);

    // Mock up the transaction request to the FHIR server
    when(fhirDataProvider.transaction(any())).thenReturn(patientMeasureReports);

    // Use legacy behavior of reifying/promoting line-level resources
    BundlerConfig config = new BundlerConfig();
    config.setReifyLineLevelResources(true);
    config.setPromoteLineLevelResources(true);

    FhirBundler bundler = new FhirBundler(config, fhirDataProvider);

    // Generate the bundle
    Bundle bundle = bundler.generateBundle(List.of(masterMeasureReport), new DocumentReference());

    // Ensure that transaction and getResourceByTypeAndId were called the expected number of times
    verify(fhirDataProvider, times(1)).transaction(any());

    // Ensure that the returned bundle meets minimum expectations for having included the patient data we expected
    Assert.assertNotNull(bundle);
    Assert.assertEquals(7, bundle.getEntry().size());
    Assert.assertEquals(Bundle.BundleType.COLLECTION, bundle.getType());
    Assert.assertNotNull(bundle.getTimestamp());
    Assert.assertEquals(ResourceType.MeasureReport, bundle.getEntry().get(0).getResource().getResourceType());
    Assert.assertEquals("http://nhsnlink.org/fhir/MeasureReport/1847296829", bundle.getEntry().get(0).getFullUrl());
    Assert.assertEquals(ResourceType.MeasureReport, bundle.getEntry().get(1).getResource().getResourceType());
    Assert.assertEquals("http://nhsnlink.org/fhir/MeasureReport/1847296829-8d99279b", bundle.getEntry().get(1).getFullUrl());
    Assert.assertEquals(ResourceType.MedicationAdministration, bundle.getEntry().get(2).getResource().getResourceType());
    Assert.assertEquals("http://nhsnlink.org/fhir/MedicationAdministration/d2f3c47c-da31-d748-9a83-e006d0f67a8b", bundle.getEntry().get(2).getFullUrl());
    Assert.assertEquals(ResourceType.MedicationAdministration, bundle.getEntry().get(3).getResource().getResourceType());
    Assert.assertEquals("http://nhsnlink.org/fhir/MedicationAdministration/7ef24f94-9148-aa29-8ed9-005693571001", bundle.getEntry().get(3).getFullUrl());
    Assert.assertEquals(ResourceType.MedicationRequest, bundle.getEntry().get(4).getResource().getResourceType());
    Assert.assertEquals("http://nhsnlink.org/fhir/MedicationRequest/075add36-e219-2327-d31b-75c5f5f12b85", bundle.getEntry().get(4).getFullUrl());
    Assert.assertEquals(ResourceType.MedicationRequest, bundle.getEntry().get(5).getResource().getResourceType());
    Assert.assertEquals("http://nhsnlink.org/fhir/MedicationRequest/04760a73-a327-05b6-9b56-344854451f60", bundle.getEntry().get(5).getFullUrl());
    Assert.assertEquals(ResourceType.Patient, bundle.getEntry().get(6).getResource().getResourceType());
    Assert.assertEquals("http://nhsnlink.org/fhir/Patient/57f91b28-7fd4-bec8-aa69-c655e42a7906", bundle.getEntry().get(6).getFullUrl());
  }

  /**
   * Add extra resources to the patient data bundle and make sure they DONT get included in the submission bundle
   */
  @Ignore()
  @Test()
  public void testBundleExcludesPatientResources() {
    // TODO
  }

  /**
   * Test a master report that includes multiple patients
   */
  @Ignore()
  @Test()
  public void testMultiplePatients() {
    // TODO
  }
}
