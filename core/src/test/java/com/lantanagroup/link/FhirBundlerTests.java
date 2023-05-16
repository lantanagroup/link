package com.lantanagroup.link;

import com.lantanagroup.link.config.bundler.BundlerConfig;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.io.InputStream;
import java.util.List;

import static org.mockito.Mockito.*;

public class FhirBundlerTests {
  private <T extends Resource> T deserializeResource(String resourcePath, Class<T> clazz) {
    InputStream measureStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
    return FhirContextProvider.getFhirContext().newJsonParser().parseResource(clazz, measureStream);
  }

  private FhirDataProvider getFhirDataProvider() {
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    Bundle patientMeasureReports = this.deserializeResource("patient-mr-bundle1.json", Bundle.class);

    ListResource census = new ListResource();
    census.setId("test-census-id");
    census.addEntry().getItem().setReference("Patient/1234");

    // Mock up the transaction request to the FHIR server
    Bundle getCensusResponse = new Bundle();
    getCensusResponse
            .addEntry()
            .setResource(census);
    when(fhirDataProvider.transaction(argThat(new ArgumentMatcher<Bundle>() {
      @Override
      public boolean matches(Bundle bundle) {
        if (bundle == null) return false;
        return bundle.getEntry().get(0).getRequest().getUrl().equals("List/test-census");
      }
    }))).thenReturn(getCensusResponse);

    when(fhirDataProvider.transaction(argThat(new ArgumentMatcher<Bundle>() {
      @Override
      public boolean matches(Bundle bundle) {
        return bundle.getEntry().get(0).getRequest().getUrl().equals("MeasureReport/1847296829-8d99279b");
      }
    }))).thenReturn(patientMeasureReports);

    return fhirDataProvider;
  }

  /**
   * Tests that a patient who is in the initial-population produces a bundle that includes contained resources
   * that are referenced in the patient measure report's "extension-supplementalData" extensions.
   */
  @Test
  public void testBundleWithReify() {
    FhirDataProvider fhirDataProvider = this.getFhirDataProvider();
    MeasureReport masterMeasureReport = this.deserializeResource("master-mr1.json", MeasureReport.class);

    // Use legacy behavior of reifying/promoting line-level resources
    BundlerConfig config = new BundlerConfig();
    config.setIncludeCensuses(true);
    config.setReifyLineLevelResources(true);
    config.setPromoteLineLevelResources(true);
    config.setOrgNpi("test-org-npi");

    FhirBundler bundler = new FhirBundler(config, fhirDataProvider);

    DocumentReference docRef = new DocumentReference();
    docRef.getContext().getRelated().add(new Reference("List/test-census"));

    // Generate the bundle
    Bundle bundle = bundler.generateBundle(List.of(masterMeasureReport), docRef);

    // Ensure that transaction and getResourceByTypeAndId were called the expected number of times
    verify(fhirDataProvider, times(2)).transaction(any());

    // Ensure that the returned bundle meets minimum expectations for having included the patient data we expected
    Assert.assertNotNull(bundle);

    Assert.assertNotNull(bundle.getIdentifier());
    Assert.assertEquals("urn:ietf:rfc:3986", bundle.getIdentifier().getSystem());
    Assert.assertTrue(bundle.getIdentifier().getValue().startsWith("urn:uuid:"));

    // All entries' fullUrl must start with the correct base url
    Assert.assertFalse(bundle.getEntry().stream().anyMatch(e -> !e.getFullUrl().startsWith("http://lantanagroup.com/fhir/nhsn-measures/")));

    Assert.assertEquals(9, bundle.getEntry().size());
    Assert.assertEquals(Bundle.BundleType.COLLECTION, bundle.getType());
    Assert.assertNotNull(bundle.getTimestamp());
    Assert.assertEquals(1, bundle.getMeta().getProfile().size());
    Assert.assertEquals("http://lantanagroup.com/fhir/nhsn-measures/StructureDefinition/nhsn-measurereport-bundle", bundle.getMeta().getProfile().get(0).getValue());

    // Organization tests
    Assert.assertEquals(ResourceType.Organization, bundle.getEntry().get(0).getResource().getResourceType());
    Organization org = (Organization) bundle.getEntry().get(0).getResource();
    Assert.assertEquals("http://lantanagroup.com/fhir/nhsn-measures/Organization/560738883", bundle.getEntry().get(0).getFullUrl());
    Assert.assertEquals(1, org.getMeta().getProfile().size());
    Assert.assertEquals("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-organization", org.getMeta().getProfile().get(0).getValue());

    // Census tests
    Assert.assertEquals(ResourceType.List, bundle.getEntry().get(1).getResource().getResourceType());
    ListResource returnedCensus = (ListResource) bundle.getEntry().get(1).getResource();
    Assert.assertEquals(1, returnedCensus.getMeta().getProfile().size());
    Assert.assertEquals("http://lantanagroup.com/fhir/nhsn-measures/StructureDefinition/poi-list", returnedCensus.getMeta().getProfile().get(0).getValue());

    // Aggregate measure report tests
    Assert.assertEquals(ResourceType.MeasureReport, bundle.getEntry().get(2).getResource().getResourceType());
    Assert.assertEquals("http://lantanagroup.com/fhir/nhsn-measures/MeasureReport/1847296829", bundle.getEntry().get(2).getFullUrl());

    // Individual measure report tests
    Assert.assertEquals(ResourceType.MeasureReport, bundle.getEntry().get(3).getResource().getResourceType());
    MeasureReport indMeasurereport = (MeasureReport) bundle.getEntry().get(3).getResource();
    Assert.assertEquals("http://lantanagroup.com/fhir/nhsn-measures/MeasureReport/1847296829-8d99279b", bundle.getEntry().get(3).getFullUrl());
    Assert.assertEquals(1, indMeasurereport.getMeta().getProfile().size());
    Assert.assertEquals("http://lantanagroup.com/fhir/nhsn-measures/StructureDefinition/individual-measure-report", indMeasurereport.getMeta().getProfile().get(0).getValue());
    Assert.assertEquals("Organization/560738883", indMeasurereport.getReporter().getReference());

    Assert.assertEquals(ResourceType.MedicationAdministration, bundle.getEntry().get(4).getResource().getResourceType());
    Assert.assertEquals("http://lantanagroup.com/fhir/nhsn-measures/MedicationAdministration/d2f3c47c-da31-d748-9a83-e006d0f67a8b", bundle.getEntry().get(4).getFullUrl());
    Assert.assertEquals(ResourceType.MedicationAdministration, bundle.getEntry().get(5).getResource().getResourceType());
    Assert.assertEquals("http://lantanagroup.com/fhir/nhsn-measures/MedicationAdministration/7ef24f94-9148-aa29-8ed9-005693571001", bundle.getEntry().get(5).getFullUrl());
    Assert.assertEquals(ResourceType.MedicationRequest, bundle.getEntry().get(6).getResource().getResourceType());
    Assert.assertEquals("http://lantanagroup.com/fhir/nhsn-measures/MedicationRequest/075add36-e219-2327-d31b-75c5f5f12b85", bundle.getEntry().get(6).getFullUrl());
    Assert.assertEquals(ResourceType.MedicationRequest, bundle.getEntry().get(7).getResource().getResourceType());
    Assert.assertEquals("http://lantanagroup.com/fhir/nhsn-measures/MedicationRequest/04760a73-a327-05b6-9b56-344854451f60", bundle.getEntry().get(7).getFullUrl());

    // Patient tests
    Assert.assertEquals(ResourceType.Patient, bundle.getEntry().get(8).getResource().getResourceType());
    Assert.assertEquals("http://lantanagroup.com/fhir/nhsn-measures/Patient/57f91b28-7fd4-bec8-aa69-c655e42a7906", bundle.getEntry().get(8).getFullUrl());
    Patient patient = (Patient) bundle.getEntry().get(8).getResource();
    Assert.assertEquals(1, patient.getMeta().getProfile().size());
    Assert.assertEquals("http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient", patient.getMeta().getProfile().get(0).getValue());
  }

  @Test
  public void testBundleWithoutReify() {
    FhirDataProvider fhirDataProvider = this.getFhirDataProvider();
    MeasureReport masterMeasureReport = this.deserializeResource("master-mr1.json", MeasureReport.class);

    // Use legacy behavior of reifying/promoting line-level resources
    BundlerConfig config = new BundlerConfig();
    config.setIncludeCensuses(true);
    config.setReifyLineLevelResources(false);
    config.setPromoteLineLevelResources(false);
    config.setOrgNpi("test-org-npi");

    FhirBundler bundler = new FhirBundler(config, fhirDataProvider);

    DocumentReference docRef = new DocumentReference();
    docRef.getContext().getRelated().add(new Reference("List/test-census"));

    // Generate the bundle
    Bundle bundle = bundler.generateBundle(List.of(masterMeasureReport), docRef);

    Assert.assertNotNull(bundle);
    Assert.assertEquals(4, bundle.getEntry().size());

    // Organization tests
    Assert.assertEquals(ResourceType.Organization, bundle.getEntry().get(0).getResource().getResourceType());
    Assert.assertEquals(ResourceType.List, bundle.getEntry().get(1).getResource().getResourceType());
    Assert.assertEquals(ResourceType.MeasureReport, bundle.getEntry().get(2).getResource().getResourceType());
    Assert.assertEquals(ResourceType.MeasureReport, bundle.getEntry().get(3).getResource().getResourceType());

    MeasureReport indMeasureReport = (MeasureReport) bundle.getEntry().get(3).getResource();
    Assert.assertEquals(MeasureReport.MeasureReportType.INDIVIDUAL, indMeasureReport.getType());

    Assert.assertTrue(indMeasureReport.getContained().stream().anyMatch(c -> c.getResourceType() == ResourceType.Patient));
    Assert.assertTrue(indMeasureReport.getContained().stream().anyMatch(c -> c.getResourceType() == ResourceType.Encounter));
    Assert.assertTrue(indMeasureReport.getContained().stream().anyMatch(c -> c.getResourceType() == ResourceType.MedicationRequest));
    Assert.assertTrue(indMeasureReport.getContained().stream().anyMatch(c -> c.getResourceType() == ResourceType.Medication));
    Assert.assertTrue(indMeasureReport.getContained().stream().anyMatch(c -> c.getResourceType() == ResourceType.Condition));
    Assert.assertTrue(indMeasureReport.getContained().stream().anyMatch(c -> c.getResourceType() == ResourceType.Observation));

    // Ensure each of the contained resources has been assigned the appropriate profile
    indMeasureReport.getContained().forEach(c -> {
      switch (c.getResourceType()) {
        case Patient:
          if (!c.getMeta().getProfile().stream().anyMatch(p -> p.getValue().equals(Constants.QiCorePatientProfileUrl))) {
            throw new Error("Expected Patient to have QI Core Patient Profile");
          }
          break;
        case Encounter:
          if (!c.getMeta().getProfile().stream().anyMatch(p -> p.getValue().equals(Constants.UsCoreEncounterProfileUrl))) {
            throw new Error("Expected Patient to have US Core Encounter Profile");
          }
          break;
        case MedicationRequest:
          if (!c.getMeta().getProfile().stream().anyMatch(p -> p.getValue().equals(Constants.UsCoreMedicationRequestProfileUrl))) {
            throw new Error("Expected Patient to have US Core MedicationRequest Profile");
          }
          break;
        case Medication:
          if (!c.getMeta().getProfile().stream().anyMatch(p -> p.getValue().equals(Constants.UsCoreMedicationProfileUrl))) {
            throw new Error("Expected Patient to have US Core Medication Profile");
          }
          break;
        case Condition:
          if (!c.getMeta().getProfile().stream().anyMatch(p -> p.getValue().equals(Constants.UsCoreConditionProfileUrl))) {
            throw new Error("Expected Patient to have US Core Condition Profile");
          }
          break;
        case Observation:
          if (!c.getMeta().getProfile().stream().anyMatch(p -> p.getValue().equals(Constants.UsCoreObservationProfileUrl))) {
            throw new Error("Expected Patient to have US Core Observation Profile");
          }
          break;
      }
    });
  }
}
