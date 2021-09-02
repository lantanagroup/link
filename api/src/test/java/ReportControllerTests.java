import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import com.lantanagroup.link.api.controller.ReportController;
import com.lantanagroup.link.model.SubjectReportModel;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

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

    Encounter enc1 = new Encounter();
    enc1.setId("encounter1");

    this.mockDocRef(untypedQuery, "report1");
    this.mockMeasureReport(fhirStoreClient, "report1", "patient1");
    IQuery<IBaseBundle> conditionQuery = this.mockSubjectBundle(untypedQuery, "Condition");
    this.mockSubjectBundle(untypedQuery, "MedicationRequest");
    this.mockSubjectBundle(untypedQuery, "Procedure");
    this.mockSubjectBundle(untypedQuery, "Encounter", enc1);

    SubjectReportModel response = reportController.getSubjectReports("report1", "patient1", authentication, request);

    // TODO: Determine how to test ICriterion
    verify(conditionQuery, times(1)).where(argThat(new CriterionArgumentMatcher(Condition.SUBJECT.hasId("patient1"))));

    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getEncounters());
    Assert.assertEquals(1, response.getEncounters().size());

    Optional<Encounter> foundEncounter = response.getEncounters().stream().filter(e -> e == enc1).findAny();
    Assert.assertEquals(true, foundEncounter.isPresent());

    // TODO: Mock additional Encounter

    // TODO: Mock and assert Condition(s)

    // TODO: Mock and assert Procedure(s)

    // TODO: Mock and assert medication request(s)
  }
}
