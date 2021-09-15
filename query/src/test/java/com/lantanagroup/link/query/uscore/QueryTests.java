package com.lantanagroup.link.query.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.query.uscore.scoop.PatientScoop;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class QueryTests {
  @Test
  public void queryTest() {
    IGenericClient fhirQueryClient = mock(IGenericClient.class);

    // Configuration for which queries should be called for each patient
    List<String> queries = new ArrayList<>();
    queries.add("Condition?patient={{patientId}}");
    queries.add("Encounter?patient={{patientId}}");
    USCoreConfig usCoreConfig = new USCoreConfig();
    usCoreConfig.setQueries(queries);

    PatientScoop patientScoop = new PatientScoop();
    patientScoop.setUsCoreConfig(usCoreConfig);
    patientScoop.setFhirQueryServer(fhirQueryClient);

    ApplicationContext applicationContext = mock(ApplicationContext.class);
    when(applicationContext.getBean(PatientScoop.class)).thenReturn(patientScoop);

    IUntypedQuery<IBaseBundle> untypedQuery = mock(IUntypedQuery.class);
    IRead read = mock(IRead.class);
    IQuery<IBaseBundle> patientBaseQuery1 = mock(IQuery.class);
    IQuery<IBaseBundle> patientBaseQuery2 = mock(IQuery.class);
    IReadTyped<Patient> readTyped = mock(IReadTyped.class);
    IQuery<IBaseBundle> conditionBaseQuery1 = mock(IQuery.class);
    IQuery<IBaseBundle> encounterBaseQuery1 = mock(IQuery.class);
    IQuery<IBaseBundle> conditionBaseQuery2 = mock(IQuery.class);
    IQuery<IBaseBundle> encounterBaseQuery2 = mock(IQuery.class);
    IQuery<IBaseBundle> conditionBaseQuery3 = mock(IQuery.class);
    IQuery<IBaseBundle> encounterBaseQuery3 = mock(IQuery.class);
    IQuery<Bundle> patientQuery1 = mock(IQuery.class);
    IQuery<Bundle> patientQuery2 = mock(IQuery.class);
    IQuery<Bundle> conditionQuery1 = mock(IQuery.class);
    IQuery<Bundle> encounterQuery1 = mock(IQuery.class);
    IQuery<Bundle> conditionQuery2 = mock(IQuery.class);
    IQuery<Bundle> encounterQuery2 = mock(IQuery.class);
    IQuery<Bundle> conditionQuery3 = mock(IQuery.class);
    IQuery<Bundle> encounterQuery3 = mock(IQuery.class);
    IReadExecutable<Patient> readExec = mock(IReadExecutable.class);

    Patient patient1 = new Patient();
    patient1.setId("patient1");
    Patient patient2 = new Patient();
    patient2.setId("patient2");
    Patient patient3 = new Patient();
    patient3.setId(new IdType("Patient", "patient3"));

    Bundle patientBundle1 = new Bundle();
    patientBundle1.addEntry().setResource(patient1);

    Bundle patientBundle2 = new Bundle();
    patientBundle2.addEntry().setResource(patient2);

    Bundle conditionBundle1 = new Bundle();
    conditionBundle1.addEntry().setResource(new Condition().setId("condition1"));
    conditionBundle1.addEntry().setResource(new Condition().setId("condition2"));

    Bundle encounterBundle1 = new Bundle();
    encounterBundle1.addEntry().setResource(new Encounter().setId("encounter1"));

    Bundle conditionBundle2 = new Bundle();

    Bundle encounterBundle2 = new Bundle();
    encounterBundle2.addEntry().setResource(new Encounter().setId("encounter2"));

    Bundle conditionBundle3 = new Bundle();
    conditionBundle3.addEntry().setResource(new Condition().setId("condition3"));

    Bundle encounterBundle3 = new Bundle();
    encounterBundle3.addEntry().setResource(new Encounter().setId("encounter3"));

    when(untypedQuery.byUrl("Patient?identifier=patientIdentifier1")).thenReturn(patientBaseQuery1);
    when(untypedQuery.byUrl("Patient?identifier=patientIdentifier2")).thenReturn(patientBaseQuery2);
    when(untypedQuery.byUrl("Condition?patient=patient1")).thenReturn(conditionBaseQuery1);
    when(untypedQuery.byUrl("Encounter?patient=patient1")).thenReturn(encounterBaseQuery1);
    when(untypedQuery.byUrl("Condition?patient=patient2")).thenReturn(conditionBaseQuery2);
    when(untypedQuery.byUrl("Encounter?patient=patient2")).thenReturn(encounterBaseQuery2);
    when(untypedQuery.byUrl("Condition?patient=patient3")).thenReturn(conditionBaseQuery3);
    when(untypedQuery.byUrl("Encounter?patient=patient3")).thenReturn(encounterBaseQuery3);
    when(read.resource(Patient.class)).thenReturn(readTyped);
    when(readTyped.withId("patient3")).thenReturn(readExec);
    when(readExec.execute()).thenReturn(patient3);

    when(patientBaseQuery1.returnBundle(Bundle.class)).thenReturn(patientQuery1);
    when(patientBaseQuery2.returnBundle(Bundle.class)).thenReturn(patientQuery2);
    when(conditionBaseQuery1.returnBundle(Bundle.class)).thenReturn(conditionQuery1);
    when(encounterBaseQuery1.returnBundle(Bundle.class)).thenReturn(encounterQuery1);
    when(conditionBaseQuery2.returnBundle(Bundle.class)).thenReturn(conditionQuery2);
    when(encounterBaseQuery2.returnBundle(Bundle.class)).thenReturn(encounterQuery2);
    when(conditionBaseQuery3.returnBundle(Bundle.class)).thenReturn(conditionQuery3);
    when(encounterBaseQuery3.returnBundle(Bundle.class)).thenReturn(encounterQuery3);

    when(patientQuery1.execute()).thenReturn(patientBundle1);
    when(patientQuery2.execute()).thenReturn(patientBundle2);
    when(conditionQuery1.execute()).thenReturn(conditionBundle1);
    when(encounterQuery1.execute()).thenReturn(encounterBundle1);
    when(conditionQuery2.execute()).thenReturn(conditionBundle2);
    when(encounterQuery2.execute()).thenReturn(encounterBundle2);
    when(conditionQuery3.execute()).thenReturn(conditionBundle3);
    when(encounterQuery3.execute()).thenReturn(encounterBundle3);

    when(fhirQueryClient.search()).thenReturn(untypedQuery);
    when(fhirQueryClient.read()).thenReturn(read);

    // Input data to the test - two entries with a business identifier and one with a logical reference
    List<PatientOfInterestModel> patientsOfInterest = new ArrayList<>();
    patientsOfInterest.add(new PatientOfInterestModel(null, "patientIdentifier1"));
    patientsOfInterest.add(new PatientOfInterestModel(null, "patientIdentifier2"));
    patientsOfInterest.add(new PatientOfInterestModel("Patient/patient3", null));

    // Execute the query
    Query theQuery = new Query();
    theQuery.setApplicationContext(applicationContext);
    theQuery.setFhirQueryClient(fhirQueryClient);
    Bundle patientDataBundle = theQuery.execute(patientsOfInterest);

    // Make sure the correct queries to the FHIR server was performed
    verify(untypedQuery, times(1)).byUrl("Patient?identifier=patientIdentifier1");
    verify(untypedQuery, times(1)).byUrl("Patient?identifier=patientIdentifier2");
    verify(untypedQuery, times(1)).byUrl("Condition?patient=patient1");
    verify(untypedQuery, times(1)).byUrl("Encounter?patient=patient1");
    verify(untypedQuery, times(1)).byUrl("Condition?patient=patient2");
    verify(untypedQuery, times(1)).byUrl("Encounter?patient=patient2");
    verify(read, times(1)).resource(Patient.class);
    verify(readTyped, times(1)).withId("patient3");

    // Make sure the patient data bundle has the expected resources in it
    Assert.assertNotNull(patientDataBundle);
    Assert.assertEquals(9, patientDataBundle.getEntry().size());

    // Make sure the patients, encounters and conditions are in the resulting bundle
    Optional<Bundle.BundleEntryComponent> foundPatient1 = patientDataBundle.getEntry().stream()
            .filter(e -> e.getResource() == patient1)
            .findAny();
    Optional<Bundle.BundleEntryComponent> foundPatient2 = patientDataBundle.getEntry().stream()
            .filter(e -> e.getResource() == patient2)
            .findAny();
    Optional<Bundle.BundleEntryComponent> foundCondition1 = patientDataBundle.getEntry().stream()
            .filter(e -> e.getResource() == conditionBundle1.getEntry().get(0).getResource())
            .findAny();
    Optional<Bundle.BundleEntryComponent> foundCondition2 = patientDataBundle.getEntry().stream()
            .filter(e -> e.getResource() == conditionBundle1.getEntry().get(1).getResource())
            .findAny();
    Optional<Bundle.BundleEntryComponent> foundEncounter1 = patientDataBundle.getEntry().stream()
            .filter(e -> e.getResource() == encounterBundle1.getEntry().get(0).getResource())
            .findAny();
    Optional<Bundle.BundleEntryComponent> foundEncounter2 = patientDataBundle.getEntry().stream()
            .filter(e -> e.getResource() == encounterBundle2.getEntry().get(0).getResource())
            .findAny();

    Assert.assertEquals(true, foundPatient1.isPresent());
    Assert.assertEquals(true, foundPatient2.isPresent());
    Assert.assertEquals(true, foundCondition1.isPresent());
    Assert.assertEquals(true, foundCondition2.isPresent());
    Assert.assertEquals(true, foundEncounter1.isPresent());
    Assert.assertEquals(true, foundEncounter2.isPresent());
  }
}
