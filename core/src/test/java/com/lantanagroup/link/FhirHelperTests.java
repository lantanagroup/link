package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FhirHelperTests {

  private FhirDataProvider fhirDataProviderTest;
  private FhirHelper fhirHelperTest;
  private FhirContext ctxTest;
  private Bundle bundleTest;
  private Bundle bundleTransactionTest;
  private Bundle bundleMeasureReportTrueTest;
  private Bundle bundleMeasureReportFalseTest;
  private MeasureReport measureReportTest;
  private MeasureReport measureReportTest2;
  private HttpServletRequest httpServletRequestTest;
  private DecodedJWT decodedJWTTest;
  private MethodOutcome outcomeTest;
  private IIdType iIdType;

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

    fhirDataProviderTest = mock(FhirDataProvider.class);
    fhirHelperTest = new FhirHelper();
    httpServletRequestTest = mock(HttpServletRequest.class);
    decodedJWTTest = mock(DecodedJWT.class);
    iIdType = mock(IIdType.class);
    iIdType.setValue("Test Audit Event");
    outcomeTest = new MethodOutcome();
    outcomeTest.setId(iIdType);

    when(decodedJWTTest.getPayload()).thenReturn("e30");
    when(fhirDataProviderTest.createOutcome(any())).thenReturn(outcomeTest);

    fhirHelperTest.recordAuditEvent(httpServletRequestTest, fhirDataProviderTest.getClient(), decodedJWTTest, FhirHelper.AuditEventTypes.Generate, "Testing String");
  }

  @Test
  public void bundleMeasureReportTest(){

    fhirDataProviderTest = mock(FhirDataProvider.class);
    fhirHelperTest = new FhirHelper();

    bundleTest = new Bundle();
    bundleTransactionTest = new Bundle();
    measureReportTest = new MeasureReport();
    measureReportTest2 = new MeasureReport();

    measureReportTest.addEvaluatedResource().setReference("Patient/patient1");
    measureReportTest.addEvaluatedResource().setReference("Condition/condition1");
    measureReportTest2.addEvaluatedResource().setReference("Patient/patient1");
    measureReportTest2.addEvaluatedResource().setReference("Condition/condition2");

    when(fhirDataProviderTest.transaction(any())).thenReturn(bundleTransactionTest);

    bundleMeasureReportTrueTest = fhirHelperTest.bundleMeasureReport(measureReportTest, fhirDataProviderTest, true);
    bundleMeasureReportFalseTest = fhirHelperTest.bundleMeasureReport(measureReportTest2, fhirDataProviderTest, false);
    Assert.assertEquals( 1, bundleMeasureReportTrueTest.getEntry().size());
    Assert.assertEquals( 1, bundleMeasureReportFalseTest.getEntry().size());
    Assert.assertNotNull(bundleMeasureReportTrueTest.getEntry().get(0).getResource());
    Assert.assertNotNull(bundleMeasureReportFalseTest.getEntry().get(0).getResource());
    MeasureReport measureEval1 = (MeasureReport) bundleMeasureReportTrueTest.getEntry().get(0).getResource();
    MeasureReport measureEval2 = (MeasureReport) bundleMeasureReportFalseTest.getEntry().get(0).getResource();
    Assert.assertEquals(2, measureEval1.getEvaluatedResource().size());
    Assert.assertEquals(2, measureEval2.getEvaluatedResource().size());
    Assert.assertEquals(measureEval2.getEvaluatedResource().get(0).getReference(), measureEval1.getEvaluatedResource().get(0).getReference());
    Assert.assertEquals("Condition/condition1", measureEval1.getEvaluatedResource().get(1).getReference());
    Assert.assertEquals("Condition/condition2", measureEval2.getEvaluatedResource().get(1).getReference());
  }

  @Test
  public void getAllPagesTest(){

    fhirDataProviderTest = mock(FhirDataProvider.class);
    ctxTest = FhirContext.forR4();
    bundleTest = new Bundle();

    Resource resourceTest = mock(Resource.class);
    bundleTest.addEntry().setResource(resourceTest);
    List<IBaseResource> bundles = FhirHelper.getAllPages(bundleTest, fhirDataProviderTest, ctxTest);
    Assert.assertEquals(1, bundles.size());
  }
}
