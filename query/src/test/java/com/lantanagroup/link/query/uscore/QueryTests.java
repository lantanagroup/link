package com.lantanagroup.link.query.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.StopwatchManager;
import com.lantanagroup.link.config.api.ApiDataStoreConfig;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.config.query.USCoreOtherResourceTypeConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class QueryTests {
  @Test
  public void queryTest() {
    IGenericClient fhirQueryClient = mock(IGenericClient.class);

    // Configuration for which queries should be called for each patient
    String measureId = "InitialInpatientPopulation";
    List<String> queries = new ArrayList<>();
    queries.add("Condition");
    queries.add("Encounter");
    queries.add("MedicationRequest");
    QueryConfig queryConfig = new QueryConfig();
    USCoreConfig usCoreConfig = new USCoreConfig();
    usCoreConfig.setPatientResourceTypes(queries);
    usCoreConfig.getPatientResourceTypes();
    List<USCoreOtherResourceTypeConfig> extraResources = new ArrayList<>();
    extraResources.add(new USCoreOtherResourceTypeConfig("Location", false, null));
    extraResources.add(new USCoreOtherResourceTypeConfig("Medication", false, null));
    usCoreConfig.setOtherResourceTypes(extraResources);
    usCoreConfig.getOtherResourceTypes();

    PatientScoop patientScoop = new PatientScoop();
    patientScoop.setStopwatchManager(new StopwatchManager());
    patientScoop.setUsCoreConfig(usCoreConfig);
    patientScoop.setQueryConfig(queryConfig);
    patientScoop.setFhirQueryServer(fhirQueryClient);
    patientScoop.setFhirDataProvider(mock(FhirDataProvider.class));

    ApplicationContext applicationContext = mock(ApplicationContext.class);
    when(applicationContext.getBean(PatientScoop.class)).thenReturn(patientScoop);

    IUntypedQuery<IBaseBundle> untypedQuery = mock(IUntypedQuery.class);
    IRead read = mock(IRead.class);
    IReadTyped<IBaseResource> medicineReadTyped = mock(IReadTyped.class);
    IReadTyped<IBaseResource> locationReadTyped = mock(IReadTyped.class);
    IReadExecutable<IBaseResource> medicineReadExecutable1 = mock(IReadExecutable.class);
    IReadExecutable<IBaseResource> medicineReadExecutable2 = mock(IReadExecutable.class);
    IReadExecutable<IBaseResource> medicineReadExecutable3 = mock(IReadExecutable.class);
    IReadExecutable<IBaseResource> locationReadExecutable1 = mock(IReadExecutable.class);
    IReadExecutable<IBaseResource> locationReadExecutable2 = mock(IReadExecutable.class);
    IReadExecutable<IBaseResource> locationReadExecutable3 = mock(IReadExecutable.class);
    IQuery<IBaseBundle> patientBaseQuery1 = mock(IQuery.class);
    IQuery<IBaseBundle> patientBaseQuery2 = mock(IQuery.class);
    IReadTyped<Patient> readTyped = mock(IReadTyped.class);
    IQuery<IBaseBundle> conditionBaseQuery1 = mock(IQuery.class);
    IQuery<IBaseBundle> encounterBaseQuery1 = mock(IQuery.class);
    IQuery<IBaseBundle> medicationRequestBaseQuery1 = mock(IQuery.class);
    IQuery<IBaseBundle> conditionBaseQuery2 = mock(IQuery.class);
    IQuery<IBaseBundle> encounterBaseQuery2 = mock(IQuery.class);
    IQuery<IBaseBundle> medicationRequestBaseQuery2 = mock(IQuery.class);
    IQuery<IBaseBundle> conditionBaseQuery3 = mock(IQuery.class);
    IQuery<IBaseBundle> encounterBaseQuery3 = mock(IQuery.class);
    IQuery<IBaseBundle> medicationRequestBaseQuery3 = mock(IQuery.class);
    IQuery<Bundle> patientQuery1 = mock(IQuery.class);
    IQuery<Bundle> patientQuery2 = mock(IQuery.class);
    IQuery<Bundle> conditionQuery1 = mock(IQuery.class);
    IQuery<Bundle> encounterQuery1 = mock(IQuery.class);
    IQuery<Bundle> medicationRequestQuery1 = mock(IQuery.class);
    IQuery<Bundle> conditionQuery2 = mock(IQuery.class);
    IQuery<Bundle> encounterQuery2 = mock(IQuery.class);
    IQuery<Bundle> medicationRequestQuery2 = mock(IQuery.class);
    IQuery<Bundle> conditionQuery3 = mock(IQuery.class);
    IQuery<Bundle> encounterQuery3 = mock(IQuery.class);
    IQuery<Bundle> medicationRequestQuery3 = mock(IQuery.class);
    IReadExecutable<Patient> readExec = mock(IReadExecutable.class);

    Patient patient1 = new Patient();
    patient1.setId("patient1");
    Patient patient2 = new Patient();
    patient2.setId("patient2");
    Patient patient3 = new Patient();
    patient3.setId(new IdType("Patient", "patient3"));

    MedicationRequest medicalRequest1 = new MedicationRequest();
    medicalRequest1.setId("medicationrequest1");
    medicalRequest1.getMedicationReference().setReference("Medication/medication1");
    MedicationRequest medicalRequest2 = new MedicationRequest();
    medicalRequest2.setId("medicationrequest2");
    medicalRequest2.getMedicationReference().setReference("Medication/medication2");
    MedicationRequest medicalRequest3 = new MedicationRequest();
    medicalRequest3.setId("medicationrequest3");
    medicalRequest3.getMedicationReference().setReference("Medication/medication3");
    MedicationRequest medicalRequest4 = new MedicationRequest();
    medicalRequest4.setId("medicationrequest4");
    medicalRequest4.getMedicationReference().setReference("Medication/medication3");

    Encounter.EncounterLocationComponent encounterLocationComponent1 = new Encounter.EncounterLocationComponent(new Reference("Location/location1"));
    List<Encounter.EncounterLocationComponent> locationList1 = new ArrayList<>();
    locationList1.add(encounterLocationComponent1);
    Encounter.EncounterLocationComponent encounterLocationComponent2 = new Encounter.EncounterLocationComponent(new Reference("Location/location2"));
    List<Encounter.EncounterLocationComponent> locationList2 = new ArrayList<>();
    locationList2.add(encounterLocationComponent2);
    Encounter.EncounterLocationComponent encounterLocationComponent3 = new Encounter.EncounterLocationComponent(new Reference("Location/location3"));
    List<Encounter.EncounterLocationComponent> locationList3 = new ArrayList<>();
    locationList3.add(encounterLocationComponent3);

    Encounter encounter1 = new Encounter();
    encounter1.setId("encounter1");
    encounter1.setLocation(locationList1);
    Encounter encounter2 = new Encounter();
    encounter2.setId("encounter2");
    encounter2.setLocation(locationList2);
    Encounter encounter3 = new Encounter();
    encounter3.setId("encounter3");
    encounter3.setLocation(locationList3);

    Bundle patientBundle1 = new Bundle();
    patientBundle1.addEntry().setResource(patient1);

    Bundle patientBundle2 = new Bundle();
    patientBundle2.addEntry().setResource(patient2);

    Bundle conditionBundle1 = new Bundle();
    conditionBundle1.addEntry().setResource(new Condition().setId("condition1"));
    conditionBundle1.addEntry().setResource(new Condition().setId("condition2"));

    Bundle medicalRequestBundle1 = new Bundle();
    medicalRequestBundle1.addEntry().setResource(medicalRequest1);

    Bundle encounterBundle1 = new Bundle();
    encounterBundle1.addEntry().setResource(encounter1);

    Bundle conditionBundle2 = new Bundle();

    Bundle encounterBundle2 = new Bundle();
    encounterBundle2.addEntry().setResource(encounter2);

    Bundle medicalRequestBundle2 = new Bundle();
    medicalRequestBundle2.addEntry().setResource(medicalRequest2);

    Bundle conditionBundle3 = new Bundle();
    conditionBundle3.addEntry().setResource(new Condition().setId("condition3"));

    Bundle encounterBundle3 = new Bundle();
    encounterBundle3.addEntry().setResource(encounter3);

    Bundle medicalRequestBundle3 = new Bundle();
    medicalRequestBundle3.addEntry().setResource(medicalRequest3);
    medicalRequestBundle3.addEntry().setResource(medicalRequest4);

    Medication medication1 = new Medication();
    medication1.setId("medication1");
    Medication medication2 = new Medication();
    medication2.setId("medication2");
    Medication medication3 = new Medication();
    medication3.setId("medication3");

    Location location1 = new Location();
    location1.setId("location1");
    Location location2 = new Location();
    location2.setId("location2");
    Location location3 = new Location();
    location3.setId("location3");

    when(untypedQuery.byUrl("Patient?identifier=patientIdentifier1")).thenReturn(patientBaseQuery1);
    when(untypedQuery.byUrl("Patient?identifier=patientIdentifier2")).thenReturn(patientBaseQuery2);
    when(untypedQuery.byUrl("Condition?patient=Patient/patient1")).thenReturn(conditionBaseQuery1);
    when(untypedQuery.byUrl("Encounter?patient=Patient/patient1")).thenReturn(encounterBaseQuery1);
    when(untypedQuery.byUrl("MedicationRequest?patient=Patient/patient1")).thenReturn(medicationRequestBaseQuery1);
    when(untypedQuery.byUrl("Condition?patient=Patient/patient2")).thenReturn(conditionBaseQuery2);
    when(untypedQuery.byUrl("Encounter?patient=Patient/patient2")).thenReturn(encounterBaseQuery2);
    when(untypedQuery.byUrl("MedicationRequest?patient=Patient/patient2")).thenReturn(medicationRequestBaseQuery2);
    when(untypedQuery.byUrl("Condition?patient=Patient/patient3")).thenReturn(conditionBaseQuery3);
    when(untypedQuery.byUrl("Encounter?patient=Patient/patient3")).thenReturn(encounterBaseQuery3);
    when(untypedQuery.byUrl("MedicationRequest?patient=Patient/patient3")).thenReturn(medicationRequestBaseQuery3);

    when(read.resource(Patient.class)).thenReturn(readTyped);
    when(readTyped.withId("patient3")).thenReturn(readExec);
    when(readExec.execute()).thenReturn(patient3);

    when(patientBaseQuery1.returnBundle(Bundle.class)).thenReturn(patientQuery1);
    when(patientBaseQuery2.returnBundle(Bundle.class)).thenReturn(patientQuery2);
    when(conditionBaseQuery1.returnBundle(Bundle.class)).thenReturn(conditionQuery1);
    when(encounterBaseQuery1.returnBundle(Bundle.class)).thenReturn(encounterQuery1);
    when(medicationRequestBaseQuery1.returnBundle(Bundle.class)).thenReturn(medicationRequestQuery1);
    when(conditionBaseQuery2.returnBundle(Bundle.class)).thenReturn(conditionQuery2);
    when(encounterBaseQuery2.returnBundle(Bundle.class)).thenReturn(encounterQuery2);
    when(medicationRequestBaseQuery2.returnBundle(Bundle.class)).thenReturn(medicationRequestQuery2);
    when(conditionBaseQuery3.returnBundle(Bundle.class)).thenReturn(conditionQuery3);
    when(encounterBaseQuery3.returnBundle(Bundle.class)).thenReturn(encounterQuery3);
    when(medicationRequestBaseQuery3.returnBundle(Bundle.class)).thenReturn(medicationRequestQuery3);

    when(patientQuery1.execute()).thenReturn(patientBundle1);
    when(patientQuery2.execute()).thenReturn(patientBundle2);
    when(conditionQuery1.execute()).thenReturn(conditionBundle1);
    when(encounterQuery1.execute()).thenReturn(encounterBundle1);
    when(medicationRequestQuery1.execute()).thenReturn(medicalRequestBundle1);
    when(conditionQuery2.execute()).thenReturn(conditionBundle2);
    when(encounterQuery2.execute()).thenReturn(encounterBundle2);
    when(medicationRequestQuery2.execute()).thenReturn(medicalRequestBundle2);
    when(conditionQuery3.execute()).thenReturn(conditionBundle3);
    when(encounterQuery3.execute()).thenReturn(encounterBundle3);
    when(medicationRequestQuery3.execute()).thenReturn(medicalRequestBundle3);

    when(fhirQueryClient.search()).thenReturn(untypedQuery);
    when(fhirQueryClient.read()).thenReturn(read);

    when(read.resource("Medication")).thenReturn(medicineReadTyped);
    when(read.resource("Location")).thenReturn(locationReadTyped);

    when(medicineReadTyped.withId("medication1")).thenReturn(medicineReadExecutable1);
    when(medicineReadTyped.withId("medication2")).thenReturn(medicineReadExecutable2);
    when(medicineReadTyped.withId("medication3")).thenReturn(medicineReadExecutable3);
    when(locationReadTyped.withId("location1")).thenReturn(locationReadExecutable1);
    when(locationReadTyped.withId("location2")).thenReturn(locationReadExecutable2);
    when(locationReadTyped.withId("location3")).thenReturn(locationReadExecutable3);

    when(medicineReadExecutable1.execute()).thenReturn(medication1);
    when(medicineReadExecutable2.execute()).thenReturn(medication2);
    when(medicineReadExecutable3.execute()).thenReturn(medication3);
    when(locationReadExecutable1.execute()).thenReturn(location1);
    when(locationReadExecutable2.execute()).thenReturn(location2);
    when(locationReadExecutable3.execute()).thenReturn(location3);

    // Input data to the test - two entries with a business identifier and one with a logical reference
    List<PatientOfInterestModel> patientsOfInterest = new ArrayList<>();
    patientsOfInterest.add(new PatientOfInterestModel(null, "patientIdentifier1"));
    patientsOfInterest.add(new PatientOfInterestModel(null, "patientIdentifier2"));
    patientsOfInterest.add(new PatientOfInterestModel("Patient/patient3", null));

    // Execute the query
    Query theQuery = new Query();
    ReportCriteria criteria = new ReportCriteria(List.of(), null, null);
    ReportContext context = new ReportContext(new FhirDataProvider(fhirQueryClient));
    theQuery.setApplicationContext(applicationContext);
    theQuery.setFhirQueryClient(fhirQueryClient);
    theQuery.execute(criteria, context, patientsOfInterest, "report1", queries, List.of(measureId));

    // Make sure the correct queries to the FHIR server was performed
    verify(untypedQuery, times(1)).byUrl("Patient?identifier=patientIdentifier1");
    verify(untypedQuery, times(1)).byUrl("Patient?identifier=patientIdentifier2");
    verify(untypedQuery, times(1)).byUrl("Condition?patient=Patient/patient1");
    verify(untypedQuery, times(1)).byUrl("Encounter?patient=Patient/patient1");
    verify(untypedQuery, times(1)).byUrl("MedicationRequest?patient=Patient/patient1");
    verify(untypedQuery, times(1)).byUrl("Condition?patient=Patient/patient2");
    verify(untypedQuery, times(1)).byUrl("Encounter?patient=Patient/patient2");
    verify(untypedQuery, times(1)).byUrl("MedicationRequest?patient=Patient/patient2");
    verify(untypedQuery, times(1)).byUrl("Condition?patient=Patient/patient3");
    verify(untypedQuery, times(1)).byUrl("Encounter?patient=Patient/patient3");

    verify(read, times(1)).resource(Patient.class);
    verify(readTyped, times(1)).withId("patient3");

    int entryCount = 0;
//    for (QueryResponse patientQueryResponse : patientQueryResponses) {
//      entryCount += patientQueryResponse.getBundle().getEntry().size();
//    }

    // Make sure the patient data bundle has the expected resources in it
    // Assert.assertNotNull(patientQueryResponses);
    //  Assert.assertEquals(19, entryCount);      // 19 instead of 20 because one medication is referenced twice

//    // Make sure the patients, encounters and conditions are in the resulting bundle
//    Optional<Bundle.BundleEntryComponent> foundPatient1 = patientQueryResponses.get(0).getBundle().getEntry().stream()
//            .filter(e -> e.getResource() == patient1)
//            .findAny();
//    Optional<Bundle.BundleEntryComponent> foundPatient2 = patientQueryResponses.get(2).getBundle().getEntry().stream()
//            .filter(e -> e.getResource() == patient2)
//            .findAny();
//    Optional<Bundle.BundleEntryComponent> foundCondition1 = patientQueryResponses.get(0).getBundle().getEntry().stream()
//            .filter(e -> e.getResource() == conditionBundle1.getEntry().get(0).getResource())
//            .findAny();
//    Optional<Bundle.BundleEntryComponent> foundCondition2 = patientQueryResponses.get(0).getBundle().getEntry().stream()
//            .filter(e -> e.getResource() == conditionBundle1.getEntry().get(1).getResource())
//            .findAny();
//    Optional<Bundle.BundleEntryComponent> foundMedicationRequest1 = patientQueryResponses.get(0).getBundle().getEntry().stream()
//            .filter(e -> e.getResource() == medicalRequestBundle1.getEntry().get(0).getResource())
//            .findAny();
//    Optional<Bundle.BundleEntryComponent> foundMedicationRequest2 = patientQueryResponses.get(2).getBundle().getEntry().stream()
//            .filter(e -> e.getResource() == medicalRequestBundle2.getEntry().get(0).getResource())
//            .findAny();
//    Optional<Bundle.BundleEntryComponent> foundEncounter1 = patientQueryResponses.get(0).getBundle().getEntry().stream()
//            .filter(e -> e.getResource() == encounterBundle1.getEntry().get(0).getResource())
//            .findAny();
//    Optional<Bundle.BundleEntryComponent> foundEncounter2 = patientQueryResponses.get(2).getBundle().getEntry().stream()
//            .filter(e -> e.getResource() == encounterBundle2.getEntry().get(0).getResource())
//            .findAny();
//    Optional<Bundle.BundleEntryComponent> foundMedication1 = patientQueryResponses.get(0).getBundle().getEntry().stream()
//            .filter(e -> e.getResource() == medication1)
//            .findAny();
//    Optional<Bundle.BundleEntryComponent> foundMedication3 = patientQueryResponses.get(1).getBundle().getEntry().stream()
//            .filter(e -> e.getResource() == medication3)
//            .findAny();
//    Optional<Bundle.BundleEntryComponent> foundLocation1 = patientQueryResponses.get(0).getBundle().getEntry().stream()
//            .filter(e -> e.getResource() == location1)
//            .findAny();

//    Assert.assertEquals(true, foundPatient1.isPresent());
//    Assert.assertEquals(true, foundPatient2.isPresent());
//    Assert.assertEquals(true, foundCondition1.isPresent());
//    Assert.assertEquals(true, foundCondition2.isPresent());
//    Assert.assertEquals(true, foundEncounter1.isPresent());
//    Assert.assertEquals(true, foundEncounter2.isPresent());
//    Assert.assertEquals(true, foundMedicationRequest1.isPresent());
//    Assert.assertEquals(true, foundMedicationRequest2.isPresent());
//    Assert.assertEquals(true, foundMedication1.isPresent());
//    Assert.assertEquals(true, foundMedication3.isPresent());
//    Assert.assertEquals(true, foundLocation1.isPresent());
  }
}
