package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import com.lantanagroup.link.EventTypes;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiConfigEvents;
import com.lantanagroup.link.mock.AuthMockInfo;
import com.lantanagroup.link.mock.MockHelper;
import com.lantanagroup.link.model.*;
import com.lantanagroup.link.nhsn.ApplyConceptMaps;
import org.apache.http.client.HttpResponseException;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class ReportControllerTests {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

//  private void mockSearchMeasure(IUntypedQuery untypedQuery, Measure measure) {
//
//
//    IQuery query1 = mock(IQuery.class);
//    IQuery query2 = mock(IQuery.class);
//    IQuery query3 = mock(IQuery.class);
//    IQuery query4 = mock(IQuery.class);
//
//    when(untypedQuery.forResource(Measure.class)).thenReturn(query1);
//    when(query1.where(any(ICriterion.class))).thenReturn(query2);
//    when(query2.returnBundle(Bundle.class)).thenReturn(query3);
//    when(query3.summaryMode(SummaryEnum.TRUE)).thenReturn(query4);
//
//    Bundle retBundle = new Bundle();
//    retBundle.setTotal(1);
//    retBundle.addEntry().setResource(measure);
//
//    when(query4.execute()).thenReturn(retBundle);
//  }

//  private void mockReadDocRef(IUntypedQuery<IBaseBundle> untypedQuery, DocumentReference docRef) {
//    IQuery<IBaseBundle> docRefQuery = mock(IQuery.class);
//    IQuery<Bundle> docRefBundleQuery = mock(IQuery.class);
//
//    Bundle docRefBundle = new Bundle();
//    docRefBundle.addEntry().setResource(docRef);
//
//    when(untypedQuery.forResource(DocumentReference.class)).thenReturn(docRefQuery);
//    when(docRefQuery.where(any(ICriterion.class))).thenReturn(docRefQuery);
//    when(docRefQuery.returnBundle(Bundle.class)).thenReturn(docRefBundleQuery);
//    when(docRefBundleQuery.cacheControl(any(CacheControlDirective.class))).thenReturn(docRefBundleQuery);
//    when(docRefBundleQuery.execute()).thenReturn(docRefBundle);
//  }
//
//  private void mockReadMeasureReport(IGenericClient fhirStoreClient, MeasureReport measureReport) {
//    IRead read = mock(IRead.class);
//    IReadTyped<MeasureReport> readTyped = mock(IReadTyped.class);
//    IReadExecutable<MeasureReport> readExecutable = mock(IReadExecutable.class);
//
//    when(fhirStoreClient.read()).thenReturn(read);
//    when(read.resource(MeasureReport.class)).thenReturn(readTyped);
//    when(readTyped.withId(measureReport.getIdElement().getIdPart())).thenReturn(readExecutable);
//    when(readExecutable.cacheControl(any(CacheControlDirective.class))).thenReturn(readExecutable);
//    when(readExecutable.execute()).thenReturn(measureReport);
//  }

  public Encounter createEncounter(String id, Reference reference) {
    Encounter encounter = new Encounter();
    encounter.setId(id);
    encounter.setSubject(reference);
    return encounter;
  }

  public Condition createCondition(String id, Reference reference) {
    Condition condition = new Condition();
    condition.setId(id);
    condition.setSubject(reference);
    return condition;
  }

  public Procedure createProcedure(String id, Reference reference){
    Procedure procedure = new Procedure();
    procedure.setId(id);
    procedure.setSubject(reference);
    return procedure;
  }

  public MedicationRequest createMedicationRequest(String id, Reference reference) {
    MedicationRequest medicationRequest = new MedicationRequest();
    medicationRequest.setId(id);
    medicationRequest.setSubject(reference);
    return medicationRequest;
  }

  public Observation createObservation(String id, Reference reference) {
    Observation observation = new Observation();
    observation.setId(id);
    observation.setSubject(reference);
    return observation;
  }

  @Ignore
  public void getSubjectReportsTest() throws Exception {
    //3 Conditions
    Condition condition1 = createCondition("http://dev-fhir/fhir/Condition/condition1/_history/1", new Reference("Patient/patient1"));
    Condition condition2 = createCondition("http://dev-fhir/fhir/Condition/condition2/_history/1", new Reference("Patient/patient1"));
    Condition condition3 = createCondition("http://dev-fhir/fhir/Condition/condition3/_history/1", new Reference("Patient/patient3"));
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    Authentication authentication = mock(Authentication.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    ReportController controller = new ReportController();
    controller.setFhirStoreProvider(fhirDataProvider);
    when(fhirDataProvider.findDocRefForReport("report1")).thenReturn(new DocumentReference());
    MeasureReport measureReport = new MeasureReport();
    measureReport.addEvaluatedResource().setReference("Condition/condition1");
    measureReport.addEvaluatedResource().setReference("Condition/condition2");
    measureReport.addEvaluatedResource().setReference("Patient/patient1");
    when(fhirDataProvider.getMeasureReportById(any())).thenReturn(measureReport);
    Bundle bundle = new Bundle();
    bundle.addEntry().setResource(condition1);
    bundle.addEntry().setResource(condition2);
    bundle.addEntry().setResource(condition3);
    when(fhirDataProvider.getResources(any(ICriterion.class), eq("Condition"))).thenReturn(bundle);
    when(fhirDataProvider.getResources(any(ICriterion.class), eq("MedicationRequest"))).thenReturn(new Bundle());
    when(fhirDataProvider.getResources(any(ICriterion.class), eq("Procedure"))).thenReturn(new Bundle());
    when(fhirDataProvider.getResources(any(ICriterion.class), eq("Encounter"))).thenReturn(new Bundle());
    when(fhirDataProvider.getResources(any(ICriterion.class), eq("Observation"))).thenReturn(new Bundle());
    PatientDataModel response = controller.getPatientData("report1", "patient1", authentication, request);
    Assert.assertNotNull(response.getConditions());
    Assert.assertNull(response.getEncounters());
    Assert.assertNull(response.getProcedures());
    Assert.assertNull(response.getMedicationRequests());
    Assert.assertNull(response.getObservations());
  }

  @Ignore
  public void excludePatientsTest() throws HttpResponseException {

    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    AuthMockInfo authMock = MockHelper.mockAuth(fhirStoreClient);
    HttpServletRequest request = mock(HttpServletRequest.class);
    ReportController controller = new ReportController();
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    controller.setFhirStoreProvider(fhirDataProvider);
    controller.setConfig(new ApiConfig());
    DocumentReference docRef = new DocumentReference();
    docRef.setId("testReportId");
    docRef.addIdentifier().setSystem("http://test.nhsnlink.org").setValue("the-measure");
    docRef.setDocStatus(DocumentReference.ReferredDocumentStatus.PRELIMINARY);
    docRef.setContext(new DocumentReference.DocumentReferenceContextComponent()
            .setPeriod(
                    new Period().setStartElement(new DateTimeType("2021-05-01T00:00:00.000-00:00"))
                            .setEndElement(new DateTimeType("2021-05-01T23:59:59.000-00:00"))));
    when(fhirDataProvider.findDocRefForReport("testReportId")).thenReturn(docRef);

    MeasureReport measureReport = new MeasureReport();
    measureReport.setId("testReportId");
    measureReport.addEvaluatedResource().setReference("Condition/condition1");
    measureReport.addEvaluatedResource().setReference("Condition/condition2");
    measureReport.addEvaluatedResource().setReference("Patient/testPatient1");
    measureReport.addEvaluatedResource().setReference("Patient/testPatient2");
    measureReport.addEvaluatedResource().setReference("Patient/testPatient3");
    measureReport.addEvaluatedResource().setReference("Patient/testPatient4");

    Measure measure = new Measure();
    measure.setId("the-measure");
    measure.addIdentifier().setSystem("http://test.nhsnlink.org").setValue("the-measure");

    when(fhirDataProvider.getMeasureReportById("testReportId")).thenReturn(measureReport);

    when(fhirDataProvider.getMeasureForReport(docRef)).thenReturn(measure);

    List<ExcludedPatientModel> excludedPatients = new ArrayList<>();
    ExcludedPatientModel excludedPatient1 = new ExcludedPatientModel();
    excludedPatient1.setPatientId("testPatient1");
    excludedPatient1.setReason(new CodeableConcept(new Coding().setCode("the-reason")));
    excludedPatients.add(excludedPatient1);
    ExcludedPatientModel excludedPatient2 = new ExcludedPatientModel();
    excludedPatient2.setPatientId("testPatient2");
    excludedPatient2.setReason(new CodeableConcept().setText("the reason"));
    excludedPatients.add(excludedPatient2);

    when(fhirDataProvider.getMeasureReport(eq("the-measure"), any(Parameters.class))).thenReturn(measureReport);

    ReportModel model = controller.excludePatients(authMock.getAuthentication(), request, authMock.getUser(), "testReportId", excludedPatients);
    Assert.assertEquals(4, model.getMeasureReport().getEvaluatedResource().size());
    Assert.assertEquals(2, model.getMeasureReport().getExtension().size());
  }


  @Test
  public void excludePatientsTestException() throws HttpResponseException {

    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    AuthMockInfo authMock = MockHelper.mockAuth(fhirStoreClient);
    HttpServletRequest request = mock(HttpServletRequest.class);
    ReportController controller = new ReportController();
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    controller.setFhirStoreProvider(fhirDataProvider);
    controller.setConfig(new ApiConfig());
    DocumentReference docRef = new DocumentReference();
    docRef.setId("testReportId");
    docRef.addIdentifier().setSystem("http://test.nhsnlink.org").setValue("the-measure");
    docRef.setDocStatus(DocumentReference.ReferredDocumentStatus.PRELIMINARY);
    docRef.setContext(new DocumentReference.DocumentReferenceContextComponent()
            .setPeriod(
                    new Period().setStartElement(new DateTimeType("2021-05-01T00:00:00.000-00:00"))
                            .setEndElement(new DateTimeType("2021-05-01T23:59:59.000-00:00"))));
    when(fhirDataProvider.findDocRefForReport("testReportId")).thenReturn(docRef);

    MeasureReport measureReport = new MeasureReport();
    measureReport.setId("testReportId");
    measureReport.addEvaluatedResource().setReference("Condition/condition1");
    measureReport.addEvaluatedResource().setReference("Condition/condition2");
    measureReport.addEvaluatedResource().setReference("Patient/testPatient1");
    measureReport.addEvaluatedResource().setReference("Patient/testPatient2");
    measureReport.addEvaluatedResource().setReference("Patient/testPatient3");

    Measure measure = new Measure();
    measure.setId("the-measure");
    measure.addIdentifier().setSystem("http://test.nhsnlink.org").setValue("the-measure");

    when(fhirDataProvider.getMeasureReportById("testReportId")).thenReturn(measureReport);

    when(fhirDataProvider.getMeasureForReport(docRef)).thenReturn(measure);

    List<ExcludedPatientModel> excludedPatients = new ArrayList<>();
    ExcludedPatientModel excludedPatient1 = new ExcludedPatientModel();
    excludedPatient1.setPatientId("testPatient1");
    excludedPatient1.setReason(new CodeableConcept(new Coding().setCode("the-reason")));
    excludedPatients.add(excludedPatient1);
    ExcludedPatientModel excludedPatient2 = new ExcludedPatientModel();
    excludedPatient2.setPatientId("testPatient4");
    excludedPatient2.setReason(new CodeableConcept().setText("the reason"));
    excludedPatients.add(excludedPatient2);

    when(fhirDataProvider.getMeasureReport(eq("the-measure"), any(Parameters.class))).thenReturn(measureReport);


    //Assert.assertEquals(4, model.getMeasureReport().getEvaluatedResource().size());
    thrown.expect(HttpResponseException.class);
    thrown.expectMessage("Patient testPatient4 is not included in report testReportId");
    ReportModel model = controller.excludePatients(authMock.getAuthentication(), request, authMock.getUser(), "testReportId", excludedPatients);
  }


  @Test
  public void triggerEvent() throws Exception {

    ReportController controller = new ReportController();
    ApiConfigEvents apiConfigEvents = new ApiConfigEvents();

    controller.setApiConfigEvents(apiConfigEvents);
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    ReportCriteria reportCriteria = mock(ReportCriteria.class);
    controller.setFhirStoreProvider(fhirDataProvider);
    Method mockMethod = ApiConfigEvents.class.getMethod("getBeforePatientDataStore");
    List<String> classes = new ArrayList();
    classes.add("com.lantanagroup.link.nhsn.ApplyConceptMaps");
    ApplyConceptMaps ac = spy(new ApplyConceptMaps());
    apiConfigEvents.setBeforeReportStore(classes);
    when(apiConfigEvents.getBeforePatientDataStore()).thenCallRealMethod();
    ReflectionTestUtils.setField(apiConfigEvents, "BeforePatientDataStore", classes);
    controller.triggerEvent(EventTypes.BeforePatientDataStore, reportCriteria, new ReportContext(fhirDataProvider));
  }

}
