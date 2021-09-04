import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import com.lantanagroup.link.api.controller.ReportController;
import com.lantanagroup.link.model.PatientDataModel;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.*;

public class ReportControllerTests {
  private void mockDocRef(IUntypedQuery<IBaseBundle> untypedQuery, String reportId) {
    IQuery<IBaseBundle> docRefQuery = mock(IQuery.class);
    IQuery<Bundle> docRefBundleQuery = mock(IQuery.class);

    Bundle docRefBundle = new Bundle();
    docRefBundle.addEntry().setResource(new DocumentReference().setId(reportId));

    when(untypedQuery.forResource(DocumentReference.class)).thenReturn(docRefQuery);
    when(docRefQuery.where(any(ICriterion.class))).thenReturn(docRefQuery);
    when(docRefQuery.returnBundle(Bundle.class)).thenReturn(docRefBundleQuery);
    when(docRefBundleQuery.cacheControl(any(CacheControlDirective.class))).thenReturn(docRefBundleQuery);
    when(docRefBundleQuery.execute()).thenReturn(docRefBundle);
  }

  private void mockMeasureReport(IGenericClient fhirStoreClient, String measureReportId, String... patientIds) {
    IRead read = mock(IRead.class);
    IReadTyped<MeasureReport> readTyped = mock(IReadTyped.class);
    IReadExecutable<MeasureReport> readExecutable = mock(IReadExecutable.class);

    MeasureReport measureReport = new MeasureReport();

    if (patientIds != null) {
      for (String patientId : patientIds) {
        measureReport.addEvaluatedResource().setReference("Patient/" + patientId);
      }
    }

    when(fhirStoreClient.read()).thenReturn(read);
    when(read.resource(MeasureReport.class)).thenReturn(readTyped);
    when(readTyped.withId(measureReportId)).thenReturn(readExecutable);
    when(readExecutable.cacheControl(any(CacheControlDirective.class))).thenReturn(readExecutable);
    when(readExecutable.execute()).thenReturn(measureReport);
  }

  private IQuery<IBaseBundle> mockSubjectBundle(IUntypedQuery<IBaseBundle> untypedQuery, String type, Resource... resResources) {
    IQuery<IBaseBundle> subBundleIntQuery = mock(IQuery.class);
    IQuery<Bundle> subBundleQuery = mock(IQuery.class);

    Bundle responseBundle = new Bundle();

    if (resResources != null) {
      for (Resource resource : resResources) {
        responseBundle.addEntry().setResource(resource);
      }
    }

    when(untypedQuery.forResource(type)).thenReturn(subBundleIntQuery);
    when(subBundleIntQuery.where(any(ICriterion.class))).thenReturn(subBundleIntQuery);
    when(subBundleIntQuery.returnBundle(Bundle.class)).thenReturn(subBundleQuery);
    when(subBundleQuery.cacheControl(any(CacheControlDirective.class))).thenReturn(subBundleQuery);
    when(subBundleQuery.execute()).thenReturn(responseBundle);

    return subBundleIntQuery;
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

  public MedicationRequest createMedicationRequest(String id, Reference reference){
    MedicationRequest medicationRequest = new MedicationRequest();
    medicationRequest.setId(id);
    medicationRequest.setSubject(reference);
    return medicationRequest;
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

    this.mockDocRef(untypedQuery, "report1");
    this.mockMeasureReport(fhirStoreClient, "report1", "patient1");

    //Mock server requests for subject bundles
    IQuery<IBaseBundle> conditionQuery = this.mockSubjectBundle(untypedQuery, "Condition", condition1, condition2, condition3);
    IQuery<IBaseBundle> medReqQuery = this.mockSubjectBundle(untypedQuery, "MedicationRequest", medReq1, medReq2);
    IQuery<IBaseBundle> procedureQuery = this.mockSubjectBundle(untypedQuery, "Procedure", proc1, proc2);
    IQuery<IBaseBundle> encounterQuery = this.mockSubjectBundle(untypedQuery, "Encounter", enc1, enc2, enc3);

    //Get subject reports
    PatientDataModel response = reportController.getPatientData("report1", "patient1", authentication, request);

    verify(conditionQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) Condition.SUBJECT.hasId("patient1"))));
    verify(medReqQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) MedicationRequest.SUBJECT.hasId("patient1"))));
    verify(procedureQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) Procedure.SUBJECT.hasId("patient1"))));
    verify(encounterQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) Encounter.SUBJECT.hasId("patient1"))));

    //Assertions
    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getConditions());
    Assert.assertNotNull(response.getMedicationRequests());
    Assert.assertNotNull(response.getProcedures());
    Assert.assertNotNull(response.getEncounters());
  }
}
