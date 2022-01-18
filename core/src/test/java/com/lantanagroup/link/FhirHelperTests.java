package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ITransaction;
import ca.uhn.fhir.rest.gclient.ITransactionTyped;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.ResourceIdChanger;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FhirHelperTests {

  private FhirDataProvider fhirDataProviderTest;
  private IGenericClient clientTest;
  private FhirHelper fhirHelperTest;
  private FhirContext ctxTest;
  private Bundle bundleTest;
  private Bundle bundleTransactionTest;
  private Bundle bundleMeasureReportTrueTest;
  private Bundle bundleMeasureReportFalseTest;
  private Bundle patientBundleResponseTest;
  private MeasureReport measureReportTest;
  private MeasureReport measureReportTest2;
  private HttpServletRequest httpServletRequestTest;
  private DecodedJWT decodedJWTTest;
  private ITransaction transactionTest;
  private ITransactionTyped<Bundle> transactionTypedBundleTest;

  @Test
  public void getNameTest() {
    HumanName name1 = new HumanName().setFamily("Sombody").addGiven("Joe");
    HumanName name2 = new HumanName().addGiven("Joe Sombody");
    HumanName name3 = new HumanName().setFamily("Joe Sombody");
    HumanName name4 = new HumanName().setText("Joe Sombody");
    HumanName name5 = new HumanName();

    String actual1 = FhirHelper.getName(Arrays.asList(name1));
    String actual2 = FhirHelper.getName(Arrays.asList(name2));
    String actual3 = FhirHelper.getName(Arrays.asList(name3));
    String actual4 = FhirHelper.getName(Arrays.asList(name4));
    String actual5 = FhirHelper.getName(Arrays.asList(name5));
    String actual6 = FhirHelper.getName(Arrays.asList());

    Assert.assertEquals(actual1, "Joe Sombody");
    Assert.assertEquals(actual2, "Joe Sombody");
    Assert.assertEquals(actual3, "Joe Sombody");
    Assert.assertEquals(actual4, "Joe Sombody");
    Assert.assertEquals(actual5, "Unknown");
    Assert.assertEquals(actual6, "Unknown");
  }

  @Test
  public void recordAuditEventTest(){
    setup();
    fhirHelperTest.recordAuditEvent(httpServletRequestTest, fhirDataProviderTest, decodedJWTTest, FhirHelper.AuditEventTypes.Generate, "Test");
  }

  @Test
  public void bundleMeasureReportTest(){
    setup();
    bundleMeasureReportTrueTest = fhirHelperTest.bundleMeasureReport(measureReportTest, fhirDataProviderTest, true);
    //bundleMeasureReportFalseTest = fhirHelperTest.bundleMeasureReport(measureReportTest2, fhirDataProviderTest, false);

  }

  @Test
  public void getAllPagesTest(){
    setup();
    Resource resourceTest = mock(Resource.class);
    bundleTest.addEntry().setResource(resourceTest);
    List<IBaseResource> bundles = FhirHelper.getAllPages(bundleTest, fhirDataProviderTest, ctxTest);
    Assert.assertEquals(1, bundles.size());
  }

  public Condition createCondition(String id, Reference reference) {
    Condition condition = new Condition();
    condition.setId(id);
    condition.setSubject(reference);
    return condition;
  }

  private void setup(){

    Condition condition1 = createCondition("http://dev-fhir/fhir/Condition/condition1/_history/1", new Reference("Patient/patient1"));
    Condition condition2 = createCondition("http://dev-fhir/fhir/Condition/condition2/_history/1", new Reference("Patient/patient1"));
    Condition condition3 = createCondition("http://dev-fhir/fhir/Condition/condition3/_history/1", new Reference("Patient/patient3"));

    fhirDataProviderTest = mock(FhirDataProvider.class);
    clientTest = mock(IGenericClient.class);
    fhirHelperTest = new FhirHelper();
    ctxTest = FhirContext.forR4();
    bundleTest = new Bundle();
    bundleTransactionTest = new Bundle();
    patientBundleResponseTest = new Bundle();
    measureReportTest = new MeasureReport();
    measureReportTest2 = new MeasureReport();
    httpServletRequestTest = mock(HttpServletRequest.class);
    decodedJWTTest = mock(DecodedJWT.class);
    transactionTest = mock(ITransaction.class);

    measureReportTest.addEvaluatedResource().setReference("Condition/condition1");
    measureReportTest.addEvaluatedResource().setReference("Condition/condition2");
    measureReportTest.addEvaluatedResource().setReference("Patient/patient1");

    when(fhirDataProviderTest.getClient()).thenReturn(clientTest);
    when(clientTest.transaction()).thenReturn(transactionTest);
    //when(transactionTest.withBundle(bundleTransactionTest)).thenReturn((ITransactionTyped<Bundle>) bundleTest);

  }
}
