package com.lantanagroup.link;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import com.lantanagroup.link.api.controller.ReportDataController;
import com.lantanagroup.link.mock.MockHelper;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ReportDataTests {

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

  @Test
  public void postReportDataTest() throws Exception{
    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    Authentication authentication = mock(Authentication.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    IUntypedQuery<IBaseBundle> untypedQuery = mock(IUntypedQuery.class);

    when(fhirStoreClient.search()).thenReturn(untypedQuery);

    ReportDataController reportDataController = new ReportDataController();
    reportDataController.setFhirStoreClient(fhirStoreClient);

    Condition condition1 = createCondition("condition1", new Reference("Patient/patient1"));
    Procedure proc1 = createProcedure("procedure1", new Reference("Patient/patient1"));
    Encounter enc1 = createEncounter("encounter1", new Reference("Patient/patient1"));

    IQuery<IBaseBundle> conditionQuery = MockHelper.mockSearchForResource(untypedQuery, "Condition", null, condition1);
    IQuery<IBaseBundle> procedureQuery = MockHelper.mockSearchForResource(untypedQuery, "Procedure", null, proc1);
    IQuery<IBaseBundle> encounterQuery = MockHelper.mockSearchForResource(untypedQuery, "Encounter", null, enc1);

    ICreate create = mock(ICreate.class);
    when(fhirStoreClient.create()).thenReturn(create);

    IUpdate update = mock(IUpdate.class);
    when(fhirStoreClient.update()).thenReturn(update);

    MockHelper.mockResourceCreation(create);
    MockHelper.mockResourceUpdate(update);

    reportDataController.storeReportData(authentication, request, "Condition", condition1);
    verify(conditionQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) Condition.IDENTIFIER.exactly().identifier("condition1"))));

    reportDataController.storeReportData(authentication, request, "Procedure", proc1);
    verify(procedureQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) Condition.IDENTIFIER.exactly().identifier("procedure1"))));

    reportDataController.storeReportData(authentication, request, "Encounter", enc1);
    verify(encounterQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) Condition.IDENTIFIER.exactly().identifier("encounter1"))));

    verify(create, times(3));
    verify(update, times(3));
  }

  @Test
  public void updateReportDataTest() throws Exception{
    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    Authentication authentication = mock(Authentication.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    IUntypedQuery<IBaseBundle> untypedQuery = mock(IUntypedQuery.class);
    MethodOutcome outcome = mock(MethodOutcome.class);

    when(fhirStoreClient.search()).thenReturn(untypedQuery);

    ReportDataController reportDataController = new ReportDataController();
    reportDataController.setFhirStoreClient(fhirStoreClient);

    Condition condition1 = createCondition("condition1", new Reference("Patient/patient1"));
    Procedure proc1 = createProcedure("procedure1", new Reference("Patient/patient1"));
    Encounter enc1 = createEncounter("encounter1", new Reference("Patient/patient1"));

    IQuery<IBaseBundle> conditionQuery = MockHelper.mockSearchForResource(untypedQuery, "Condition", null, condition1);
    IQuery<IBaseBundle> procedureQuery = MockHelper.mockSearchForResource(untypedQuery, "Procedure", null, proc1);
    IQuery<IBaseBundle> encounterQuery = MockHelper.mockSearchForResource(untypedQuery, "Encounter", null, enc1);

    condition1 = createCondition("condition1", new Reference("Patient/patient2"));
    proc1 = createProcedure("procedure1", new Reference("Patient/patient2"));
    enc1 = createEncounter("encounter1", new Reference("Patient/patient2"));

    IUpdate update = mock(IUpdate.class);
    when(fhirStoreClient.update()).thenReturn(update);

    MockHelper.mockResourceUpdate(update);

    reportDataController.updateReportData(authentication, request, "Condition", "condition1", condition1);
    verify(conditionQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) Condition.IDENTIFIER.exactly().identifier("condition1"))));

    reportDataController.updateReportData(authentication, request, "Procedure", "procedure1", proc1);
    verify(procedureQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) Condition.IDENTIFIER.exactly().identifier("procedure1"))));

    reportDataController.updateReportData(authentication, request, "Encounter", "encounter1", enc1);
    verify(encounterQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) Condition.IDENTIFIER.exactly().identifier("encounter1"))));

    verify(update, times(3));
  }

  @Test
  public void getReportData() throws Exception{
    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    Authentication authentication = mock(Authentication.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    IUntypedQuery<IBaseBundle> untypedQuery = mock(IUntypedQuery.class);

    when(fhirStoreClient.search()).thenReturn(untypedQuery);

    ReportDataController reportDataController = new ReportDataController();
    reportDataController.setFhirStoreClient(fhirStoreClient);

    Condition condition1 = createCondition("condition1", new Reference("Patient/patient1"));
    Procedure proc1 = createProcedure("procedure1", new Reference("Patient/patient1"));
    Encounter enc1 = createEncounter("encounter1", new Reference("Patient/patient1"));

    IQuery<IBaseBundle> conditionQuery = MockHelper.mockSearchForResource(untypedQuery, "Condition", null, condition1);
    IQuery<IBaseBundle> procedureQuery = MockHelper.mockSearchForResource(untypedQuery, "Procedure", null, proc1);
    IQuery<IBaseBundle> encounterQuery = MockHelper.mockSearchForResource(untypedQuery, "Encounter", null, enc1);

    reportDataController.getReportData(authentication, request, "Condition");
    verify(conditionQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) Condition.IDENTIFIER.exactly().identifier("condition1"))));

    reportDataController.getReportData(authentication, request, "Procedure");
    verify(procedureQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) Procedure.IDENTIFIER.exactly().identifier("procedure1"))));

    reportDataController.getReportData(authentication, request, "Encounter");
    verify(encounterQuery, times(1)).where((ICriterion<?>) argThat(new CriterionArgumentMatcher((ICriterionInternal) Condition.IDENTIFIER.exactly().identifier("encounter1"))));
  }
}
