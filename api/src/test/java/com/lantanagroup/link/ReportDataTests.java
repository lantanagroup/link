package com.lantanagroup.link;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreate;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.gclient.IUpdate;
import com.lantanagroup.link.api.controller.ReportDataController;
import com.lantanagroup.link.mock.MockHelper;
import org.apache.http.client.HttpResponseException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
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
  public void postReportDataFailTest() throws Exception{
    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    Authentication authentication = mock(Authentication.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    IUntypedQuery<IBaseBundle> untypedQuery = mock(IUntypedQuery.class);

    when(fhirStoreClient.search()).thenReturn(untypedQuery);

    ReportDataController reportDataController = new ReportDataController();
    reportDataController.setFhirStoreClient(fhirStoreClient);

    Condition cond1 = createCondition("condition1", new Reference("Patient/patient1"));
    Procedure proc1 = createProcedure("procedure1", new Reference("Patient/patient1"));
    Encounter enc1 = createEncounter("encounter1", new Reference("Patient/patient1"));

    MockHelper.mockSearchForResource(untypedQuery, "Condition", null, cond1);
    MockHelper.mockSearchForResource(untypedQuery, "Procedure", null, proc1);
    MockHelper.mockSearchForResource(untypedQuery, "Encounter", null, enc1);

    ICreate create = mock(ICreate.class);
    when(fhirStoreClient.create()).thenReturn(create);

    IUpdate update = mock(IUpdate.class);
    when(fhirStoreClient.update()).thenReturn(update);

    MockHelper.mockResourceCreation(create, cond1);
    MockHelper.mockResourceUpdate(update, cond1);

    try {
      reportDataController.storeReportData(authentication, request, "Condition", cond1);
    }
    catch(HttpResponseException e){
      Assert.assertEquals("status code: 500, reason phrase: Resource with id condition1 already exists", e.getMessage());
    }

    MockHelper.mockResourceCreation(create, proc1);
    MockHelper.mockResourceUpdate(update, proc1);

    try {
      reportDataController.storeReportData(authentication, request, "Procedure", proc1);
    }
    catch(HttpResponseException e){
      Assert.assertEquals("status code: 500, reason phrase: Resource with id procedure1 already exists", e.getMessage());
    }

    MockHelper.mockResourceCreation(create, enc1);
    MockHelper.mockResourceUpdate(update, enc1);

    try {
      reportDataController.storeReportData(authentication, request, "Encounter", enc1);
    }
    catch(HttpResponseException e){
      Assert.assertEquals("status code: 500, reason phrase: Resource with id encounter1 already exists", e.getMessage());
    }

    verify(create, times(0)).resource(any(Condition.class));
    verify(create, times(0)).resource(any(Procedure.class));
    verify(create, times(0)).resource(any(Encounter.class));
  }

  @Ignore
  @Test
  public void postReportDataSuccessTest() throws Exception{
    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    Authentication authentication = mock(Authentication.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    IUntypedQuery<IBaseBundle> untypedQuery = mock(IUntypedQuery.class);

    when(fhirStoreClient.search()).thenReturn(untypedQuery);

    ReportDataController reportDataController = new ReportDataController();
    reportDataController.setFhirStoreClient(fhirStoreClient);

    Condition cond1 = createCondition("condition1", new Reference("Patient/patient1"));
    Procedure proc1 = createProcedure("procedure1", new Reference("Patient/patient1"));
    Encounter enc1 = createEncounter("encounter1", new Reference("Patient/patient1"));

    MockHelper.mockFailSearchForResource(untypedQuery, "Condition");
    MockHelper.mockFailSearchForResource(untypedQuery, "Procedure");
    MockHelper.mockFailSearchForResource(untypedQuery, "Encounter");

    ICreate create = mock(ICreate.class);
    when(fhirStoreClient.create()).thenReturn(create);

    MockHelper.mockResourceCreation(create, cond1);

    reportDataController.storeReportData(authentication, request, "Condition", cond1);

    MockHelper.mockResourceCreation(create, proc1);

    reportDataController.storeReportData(authentication, request, "Procedure", proc1);

    MockHelper.mockResourceCreation(create, enc1);

    reportDataController.storeReportData(authentication, request, "Encounter", enc1);

    verify(create, times(1)).resource(any(Condition.class));
    verify(create, times(1)).resource(any(Procedure.class));
    verify(create, times(1)).resource(any(Encounter.class));
  }

  @Test
  public void updateReportDataTest() throws Exception{
    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    Authentication authentication = mock(Authentication.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    IUntypedQuery<IBaseBundle> untypedQuery = mock(IUntypedQuery.class);

    when(fhirStoreClient.search()).thenReturn(untypedQuery);

    ReportDataController reportDataController = new ReportDataController();
    reportDataController.setFhirStoreClient(fhirStoreClient);

    Condition cond1 = createCondition("condition1", new Reference("Patient/patient1"));
    Procedure proc1 = createProcedure("procedure1", new Reference("Patient/patient1"));
    Encounter enc1 = createEncounter("encounter1", new Reference("Patient/patient1"));

    MockHelper.mockSearchForResource(untypedQuery, "Condition", null, cond1);
    MockHelper.mockSearchForResource(untypedQuery, "Procedure", null, proc1);
    MockHelper.mockSearchForResource(untypedQuery, "Encounter", null, enc1);

    cond1 = createCondition("condition1", new Reference("Patient/patient2"));
    proc1 = createProcedure("procedure1", new Reference("Patient/patient2"));
    enc1 = createEncounter("encounter1", new Reference("Patient/patient2"));

    IUpdate update = mock(IUpdate.class);
    when(fhirStoreClient.update()).thenReturn(update);

    MockHelper.mockResourceUpdate(update, cond1);

    reportDataController.updateReportData(authentication, request, "Condition", "condition1", cond1);

    MockHelper.mockResourceUpdate(update, proc1);

    reportDataController.updateReportData(authentication, request, "Procedure", "procedure1", proc1);

    MockHelper.mockResourceUpdate(update, enc1);

    reportDataController.updateReportData(authentication, request, "Encounter", "encounter1", enc1);

    verify(update, times(1)).resource(any(Condition.class));
    verify(update, times(1)).resource(any(Procedure.class));
    verify(update, times(1)).resource(any(Encounter.class));
  }
}
