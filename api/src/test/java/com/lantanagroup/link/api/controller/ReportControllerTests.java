package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.JsonParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.EventTypes;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiConfigEvents;
import com.lantanagroup.link.mock.AuthMockInfo;
import com.lantanagroup.link.mock.MockHelper;
import com.lantanagroup.link.model.*;
import com.lantanagroup.link.nhsn.FHIRReceiver;
import org.apache.http.client.HttpResponseException;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class ReportControllerTests {
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
  public void getReportPatientsTest() throws Exception {
    //2 Patients
    DocumentReference docRef = new DocumentReference();
    Identifier identifier = new Identifier();
    identifier.setValue("report1");
    String content = "content";
    docRef.setMasterIdentifier(identifier);
    DocumentReference.DocumentReferenceContentComponent documentReferenceContentComponent = new DocumentReference.DocumentReferenceContentComponent();
    Attachment attachment = new Attachment();
    attachment.setUrl(content);
    documentReferenceContentComponent.setAttachment(attachment);
    List<DocumentReference.DocumentReferenceContentComponent> documentReferenceContentComponentList = new ArrayList<>();
    documentReferenceContentComponentList.add(documentReferenceContentComponent);
    docRef.setContent(documentReferenceContentComponentList);
    MedicationRequest medicationRequest1 = new MedicationRequest();
    MedicationRequest medicationRequest2 = new MedicationRequest();
    Encounter encounter1 = new Encounter();
    Patient patient1 = new Patient();
    Patient patient2 = new Patient();
    patient1.setId("Patient/patient1");
    patient2.setId("Patient/patient2");
    medicationRequest1.getSubject().setReference("Patient/patient1");
    medicationRequest2.getSubject().setReference("Patient/patient2");
    encounter1.getSubject().setReference("Patient/patient1");
    MeasureReport measureReport1 = new MeasureReport();
    MeasureReport measureReport2 = new MeasureReport();
    measureReport1.setId("MeasureReport/patient1");
    measureReport2.setId("MeasureReport/patient2");
    measureReport1.addEvaluatedResource(new Reference("Patient/patient1"));
    measureReport1.addEvaluatedResource(new Reference("MedicationRequest/patient1"));
    measureReport1.addEvaluatedResource(new Reference("Encounter/patient1"));
    measureReport2.addEvaluatedResource(new Reference("Patient/patient2"));
    measureReport2.addEvaluatedResource(new Reference("MedicationRequest/patient2"));
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    FHIRReceiver receiver = mock(FHIRReceiver.class);
    FhirContext cxt = mock(FhirContext.class);
    IParser jsonParser = mock(JsonParser.class);
    ApplicationContext context = mock(ApplicationContext.class);
    ReportController controller = new ReportController();
    controller.setContext(context);
    controller.setFhirStoreProvider(fhirDataProvider);
    controller.setCtx(cxt);
    when(fhirDataProvider.findDocRefForReport("report1")).thenReturn(docRef);
    MeasureReport MasterMeasureReport = new MeasureReport();
    MasterMeasureReport.addEvaluatedResource(new Reference("MeasureReport/patient1"));
    MasterMeasureReport.addEvaluatedResource(new Reference("MeasureReport/patient2"));
    when(fhirDataProvider.getMeasureReportById("report1")).thenReturn(MasterMeasureReport);
    Bundle bundle = new Bundle();
    bundle.addEntry().setResource(measureReport1);
    bundle.addEntry().setResource(measureReport2);
    when(context.getBean(FHIRReceiver.class)).thenReturn(receiver);
    when(receiver.retrieveContent(anyString())).thenReturn(bundle);
    when(fhirDataProvider.tryGetResource("MedicationRequest", "patient1")).thenReturn(medicationRequest1);
    when(fhirDataProvider.tryGetResource("MedicationRequest", "patient2")).thenReturn(medicationRequest2);
    when(fhirDataProvider.tryGetResource("Patient", "patient1")).thenReturn(patient1);
    when(fhirDataProvider.tryGetResource("Patient", "patient2")).thenReturn(patient2);
    when(fhirDataProvider.tryGetResource("Encounter", "patient1")).thenReturn(encounter1);
    List<PatientReportModel> reports = controller.getReportPatients("report1");
    Assert.assertEquals(reports.size(), 2);
  }

  @Ignore
  public void getSubjectReportsTest() throws Exception {
    //1 MedicationRequest and 1 Encounter
    DocumentReference docRef = new DocumentReference();
    Identifier identifier = new Identifier();
    identifier.setValue("report1");
    String content = "content";
    docRef.setMasterIdentifier(identifier);
    DocumentReference.DocumentReferenceContentComponent documentReferenceContentComponent = new DocumentReference.DocumentReferenceContentComponent();
    Attachment attachment = new Attachment();
    attachment.setUrl(content);
    documentReferenceContentComponent.setAttachment(attachment);
    List<DocumentReference.DocumentReferenceContentComponent> documentReferenceContentComponentList = new ArrayList<>();
    documentReferenceContentComponentList.add(documentReferenceContentComponent);
    docRef.setContent(documentReferenceContentComponentList);
    MedicationRequest medicationRequest1 = new MedicationRequest();
    MedicationRequest medicationRequest2 = new MedicationRequest();
    Encounter encounter1 = new Encounter();
    Patient patient1 = new Patient();
    Patient patient2 = new Patient();
    patient1.setId("Patient/patient1");
    patient2.setId("Patient/patient2");
    medicationRequest1.getSubject().setReference("Patient/patient1");
    medicationRequest2.getSubject().setReference("Patient/patient2");
    encounter1.getSubject().setReference("Patient/patient1");
    MeasureReport measureReport1 = new MeasureReport();
    MeasureReport measureReport2 = new MeasureReport();
    measureReport1.setId("MeasureReport/patient1");
    measureReport2.setId("MeasureReport/patient2");
    measureReport1.addEvaluatedResource(new Reference("Patient/patient1"));
    measureReport1.addEvaluatedResource(new Reference("MedicationRequest/patient1"));
    measureReport1.addEvaluatedResource(new Reference("Encounter/patient1"));
    measureReport2.addEvaluatedResource(new Reference("Patient/patient2"));
    measureReport2.addEvaluatedResource(new Reference("MedicationRequest/patient2"));
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    FHIRReceiver receiver = mock(FHIRReceiver.class);
    FhirContext cxt = mock(FhirContext.class);
    IParser jsonParser = mock(JsonParser.class);
    Authentication authentication = mock(Authentication.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    ApplicationContext context = mock(ApplicationContext.class);
    ReportController controller = new ReportController();
    controller.setContext(context);
    controller.setFhirStoreProvider(fhirDataProvider);
    controller.setCtx(cxt);
    when(fhirDataProvider.findDocRefForReport("report1")).thenReturn(docRef);
    MeasureReport MasterMeasureReport = new MeasureReport();
    MasterMeasureReport.addEvaluatedResource(new Reference("MeasureReport/patient1"));
    MasterMeasureReport.addEvaluatedResource(new Reference("MeasureReport/patient2"));
    when(fhirDataProvider.getMeasureReportById("report1")).thenReturn(MasterMeasureReport);
    Bundle bundle = new Bundle();
    bundle.addEntry().setResource(measureReport1);
    bundle.addEntry().setResource(measureReport2);
    when(context.getBean(FHIRReceiver.class)).thenReturn(receiver);
    when(receiver.retrieveContent(anyString())).thenReturn(bundle);
    when(fhirDataProvider.tryGetResource("MedicationRequest", "patient1")).thenReturn(medicationRequest1);
    when(fhirDataProvider.tryGetResource("MedicationRequest", "patient2")).thenReturn(medicationRequest2);
    when(fhirDataProvider.tryGetResource("Patient", "patient1")).thenReturn(patient1);
    when(fhirDataProvider.tryGetResource("Patient", "patient2")).thenReturn(patient2);
    when(fhirDataProvider.tryGetResource("Encounter", "patient1")).thenReturn(encounter1);
    PatientDataModel response = controller.getPatientData("report1", "patient1", authentication, request);
    Assert.assertEquals(response.getConditions().size(), 0);
    Assert.assertEquals(response.getEncounters().size(), 1);
    Assert.assertEquals(response.getProcedures().size(), 0);
    Assert.assertEquals(response.getMedicationRequests().size(), 1);
    Assert.assertEquals(response.getObservations().size(), 0);
  }

  @Ignore
  public void excludePatientsTest() throws Exception {

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
    Assert.assertEquals(4, model.getReportMeasureList().get(0).getMeasureReport().getEvaluatedResource().size());
    Assert.assertEquals(2, model.getReportMeasureList().get(0).getMeasureReport().getExtension().size());
  }


  @Ignore
  public void excludePatientsTestException() throws Exception {

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
    Assert.assertThrows("Patient testPatient4 is not included in report testReportId", HttpResponseException.class, () -> {
      ReportModel model = controller.excludePatients(authMock.getAuthentication(), request, authMock.getUser(), "testReportId", excludedPatients);
    });
  }


}
