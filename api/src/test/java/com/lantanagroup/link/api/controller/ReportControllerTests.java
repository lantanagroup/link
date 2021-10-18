package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import com.lantanagroup.link.CriterionArgumentMatcher;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.mock.AuthMockInfo;
import com.lantanagroup.link.mock.MockHelper;
import com.lantanagroup.link.mock.TransactionMock;
import com.lantanagroup.link.model.ExcludedPatientModel;
import com.lantanagroup.link.model.PatientDataModel;
import org.apache.http.client.HttpResponseException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class ReportControllerTests {
  private void mockSearchMeasure(IUntypedQuery untypedQuery, Measure measure) {
    IQuery query1 = mock(IQuery.class);
    IQuery query2 = mock(IQuery.class);
    IQuery query3 = mock(IQuery.class);
    IQuery query4 = mock(IQuery.class);

    when(untypedQuery.forResource(Measure.class)).thenReturn(query1);
    when(query1.where(any(ICriterion.class))).thenReturn(query2);
    when(query2.returnBundle(Bundle.class)).thenReturn(query3);
    when(query3.summaryMode(SummaryEnum.TRUE)).thenReturn(query4);

    Bundle retBundle = new Bundle();
    retBundle.setTotal(1);
    retBundle.addEntry().setResource(measure);

    when(query4.execute()).thenReturn(retBundle);
  }

  private void mockReadDocRef(IUntypedQuery<IBaseBundle> untypedQuery, DocumentReference docRef) {
    IQuery<IBaseBundle> docRefQuery = mock(IQuery.class);
    IQuery<Bundle> docRefBundleQuery = mock(IQuery.class);

    Bundle docRefBundle = new Bundle();
    docRefBundle.addEntry().setResource(docRef);

    when(untypedQuery.forResource(DocumentReference.class)).thenReturn(docRefQuery);
    when(docRefQuery.where(any(ICriterion.class))).thenReturn(docRefQuery);
    when(docRefQuery.returnBundle(Bundle.class)).thenReturn(docRefBundleQuery);
    when(docRefBundleQuery.cacheControl(any(CacheControlDirective.class))).thenReturn(docRefBundleQuery);
    when(docRefBundleQuery.execute()).thenReturn(docRefBundle);
  }

  private void mockReadMeasureReport(IGenericClient fhirStoreClient, MeasureReport measureReport) {
    IRead read = mock(IRead.class);
    IReadTyped<MeasureReport> readTyped = mock(IReadTyped.class);
    IReadExecutable<MeasureReport> readExecutable = mock(IReadExecutable.class);

    when(fhirStoreClient.read()).thenReturn(read);
    when(read.resource(MeasureReport.class)).thenReturn(readTyped);
    when(readTyped.withId(measureReport.getIdElement().getIdPart())).thenReturn(readExecutable);
    when(readExecutable.cacheControl(any(CacheControlDirective.class))).thenReturn(readExecutable);
    when(readExecutable.execute()).thenReturn(measureReport);
  }

  public Encounter createEncounter(String id, Reference reference){
    Encounter encounter = new Encounter();
    encounter.setId(id);
    encounter.setSubject(reference);
    return encounter;
  }

  public Condition createCondition(String id, Reference reference){
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


  @Test
  public void getSubjectReportsTest() throws Exception {
    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    Authentication authentication = mock(Authentication.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    IUntypedQuery<IBaseBundle> untypedQuery = mock(IUntypedQuery.class);

    when(fhirStoreClient.search()).thenReturn(untypedQuery);

    ReportController reportController = new ReportController();
    reportController.setFhirStoreClient(fhirStoreClient);

    MeasureReport measureReport = new MeasureReport();
    measureReport.setId("report1");
    measureReport.addEvaluatedResource().setReference("Patient/patient1");

    //3 Conditions
    Condition condition1 = createCondition("condition1", new Reference("Patient/patient1"));
    Condition condition2 = createCondition("condition2", new Reference("Patient/patient1"));
    Condition condition3 = createCondition("condition3", new Reference("Patient/patient1"));

    //2 MedicationRequests
    MedicationRequest medReq1 = createMedicationRequest("medReq1", new Reference("Patient/patient1"));
    MedicationRequest medReq2 = createMedicationRequest("medReq2", new Reference("Patient/patient1"));

    //2 Procedures
    Procedure proc1 = createProcedure("procedure1", new Reference("Patient/patient1"));
    Procedure proc2 = createProcedure("procedure2", new Reference("Patient/patient1"));

    //3 Encounters
    Encounter enc1 = createEncounter("encounter1", new Reference("Patient/patient1"));
    Encounter enc2 = createEncounter("encounter2", new Reference("Patient/patient1"));
    Encounter enc3 = createEncounter("encounter3", new Reference("Patient/patient1"));

    DocumentReference docRef = new DocumentReference();
    docRef.setId("report1");

    this.mockReadDocRef(untypedQuery, docRef);
    this.mockReadMeasureReport(fhirStoreClient, measureReport);

    //Mock server requests for subject bundles
    IQuery<IBaseBundle> conditionQuery = MockHelper.mockSearchForResource(untypedQuery, "Condition", null, condition1, condition2, condition3);
    IQuery<IBaseBundle> medReqQuery = MockHelper.mockSearchForResource(untypedQuery, "MedicationRequest", null, medReq1, medReq2);
    IQuery<IBaseBundle> procedureQuery = MockHelper.mockSearchForResource(untypedQuery, "Procedure", null, proc1, proc2);
    IQuery<IBaseBundle> encounterQuery = MockHelper.mockSearchForResource(untypedQuery, "Encounter", null, enc1, enc2, enc3);
    IQuery<IBaseBundle> observationQuery = MockHelper.mockSearchForResource(untypedQuery, "Observation", null);

    //Get subject reports
    PatientDataModel response = reportController.getPatientData("report1", "patient1", authentication, request);

    verify(conditionQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) Condition.SUBJECT.hasId("patient1"))));
    verify(medReqQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) MedicationRequest.SUBJECT.hasId("patient1"))));
    verify(procedureQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) Procedure.SUBJECT.hasId("patient1"))));
    verify(encounterQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) Encounter.SUBJECT.hasId("patient1"))));

    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getConditions());
    Assert.assertNotNull(response.getMedicationRequests());
    Assert.assertNotNull(response.getProcedures());
    Assert.assertNotNull(response.getEncounters());
  }

  @Test
  public void excludePatientsTest() throws HttpResponseException {
    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    IUntypedQuery untypedQuery = mock(IUntypedQuery.class);
    AuthMockInfo authMock = MockHelper.mockAuth(fhirStoreClient);
    HttpServletRequest req = mock(HttpServletRequest.class);
    ReportController controller = new ReportController();
    controller.setFhirStoreClient(fhirStoreClient);
    controller.setConfig(new ApiConfig());

    when(fhirStoreClient.search()).thenReturn(untypedQuery);

    List<ExcludedPatientModel> excludedPatients = new ArrayList<>();
    ExcludedPatientModel excludedPatient1 = new ExcludedPatientModel();
    excludedPatient1.setPatientId("testPatient1");
    excludedPatient1.setReason(new CodeableConcept(new Coding().setCode("the-reason")));
    excludedPatients.add(excludedPatient1);
    ExcludedPatientModel excludedPatient2 = new ExcludedPatientModel();
    excludedPatient2.setPatientId("testPatient2");
    excludedPatient2.setReason(new CodeableConcept().setText("the reason"));
    excludedPatients.add(excludedPatient2);

    DocumentReference docRef = new DocumentReference();
    docRef.setId("testReportId");
    docRef.addIdentifier().setSystem("http://test.nhsnlink.org").setValue("the-measure");
    docRef.setDocStatus(DocumentReference.ReferredDocumentStatus.PRELIMINARY);
    docRef.setContext(new DocumentReference.DocumentReferenceContextComponent()
            .setPeriod(
                    new Period().setStartElement(new DateTimeType("2021-05-01T00:00:00.000-00:00"))
                            .setEndElement(new DateTimeType("2021-05-01T23:59:59.000-00:00"))));

    MeasureReport measureReport = new MeasureReport();
    measureReport.setId("testReportId");
    measureReport.addEvaluatedResource().setReference("Patient/testPatient1");
    measureReport.addEvaluatedResource().setReference("Patient/testPatient2");

    Measure measure = new Measure();
    measure.setId("the-measure");
    measure.addIdentifier().setSystem("http://test.nhsnlink.org").setValue("the-measure");

    this.mockReadDocRef(untypedQuery, docRef);
    this.mockReadMeasureReport(fhirStoreClient, measureReport);
    this.mockSearchMeasure(untypedQuery, measure);

    ITransaction transaction = mock(ITransaction.class);
    when(fhirStoreClient.transaction()).thenReturn(transaction);

    // Mock the transaction call to update the MeasureReport and DELETE the FHIR Patient resources
    TransactionMock transactionMock1 = new TransactionMock(new ArgumentMatcher<Bundle>() {
      @Override
      public boolean matches(Bundle b) {
        if (b == null || b.getEntry().size() != 1) return false;
        if (b.getEntryFirstRep().getResource() == null) return false;
        if (!(b.getEntryFirstRep().getResource() instanceof MeasureReport)) return false;
        MeasureReport mr = (MeasureReport) b.getEntryFirstRep().getResource();
        if (!mr.getIdElement().getIdPart().equals("testReport1"));
        if (mr.getEvaluatedResource().size() != 0) return false;
        return true;
      }
    }, new Bundle());
    // Mock the transaction call to update the MeasureReport and DocumentReference after the measure was re-evaluated
    TransactionMock transactionMock2 = new TransactionMock(new ArgumentMatcher<Bundle>() {
      @Override
      public boolean matches(Bundle b) {
        if (b == null || b.getEntry().size() != 2) return false;
        Boolean foundMeasureReport = b.getEntry().stream().anyMatch(e -> e.getResource() instanceof MeasureReport &&
                e.getResource().getIdElement().getIdPart().equals("testReportId"));
        Boolean foundDocRef = b.getEntry().stream().anyMatch(e -> e.getResource() instanceof DocumentReference &&
                e.getResource().getIdElement().getIdPart().equals("testReportId"));
        return foundMeasureReport && foundDocRef;
      }
    }, new Bundle());
    MockHelper.mockTransaction(transaction, transactionMock1, transactionMock2);

    // Mock the $evaluate-measure operation call
    MockHelper.mockInstanceOperation(fhirStoreClient, "$evaluate-measure", new ArgumentMatcher<IIdType>() {
      @Override
      public boolean matches(IIdType idType) {
        return idType.toString().equals("Measure/the-measure");
      }
    }, new ArgumentMatcher<Parameters>() {
      @Override
      public boolean matches(Parameters parameters) {
        if (parameters.getParameter().size() != 2) return false;
        if (parameters.getParameter("periodStart") == null) return false;
        if (parameters.getParameter("periodEnd") == null) return false;
        // TODO: Check the value of periodStart and periodEnd
        return true;
      }
    }, new MeasureReport());

    // Execute the main excludePatients() call on the controller. This is what kicks off testing against the mocks above
    controller.excludePatients(
            authMock.getAuthentication(),
            req,
            authMock.getUser(),
            "testReportId",
            excludedPatients);

    // TODO: Perform some verify() checks
  }
}
