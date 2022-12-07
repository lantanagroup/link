package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.mock.AuthMockInfo;
import com.lantanagroup.link.mock.MockHelper;
import com.lantanagroup.link.model.StoredMeasure;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportDefinitionControllerTest {

  private FhirDataProvider fhirDataProvider;
  private IGenericClient fhirStoreClient;
  private AuthMockInfo authMock;
  private ReportDefinitionController reportDefinitionControllerTest;
  private HttpServletRequest request;
  private Measure measureTest;
  private Bundle measureBundleTest;
  private Bundle responseBundleTest;
  private Bundle.BundleEntryComponent measureEntryComponentTest;
  private List<Bundle.BundleEntryComponent> componentListTest;

  @Test
  public void getMeasures() throws Exception {

    fhirDataProvider = mock(FhirDataProvider.class);
    fhirStoreClient = mock(IGenericClient.class);
    authMock = MockHelper.mockAuth(fhirStoreClient);
    reportDefinitionControllerTest = new ReportDefinitionController();
    request = mock(HttpServletRequest.class);

    measureTest = new Measure();
    measureBundleTest = new Bundle();
    measureEntryComponentTest = new Bundle.BundleEntryComponent();
    componentListTest = new ArrayList<>();
    responseBundleTest = new Bundle();

    reportDefinitionControllerTest.setFhirStoreProvider(fhirDataProvider);
    reportDefinitionControllerTest.setConfig(new ApiConfig());

    measureTest.setTitle("testMeasureTitle");
    measureBundleTest.addEntry().setResource(measureTest);
    measureEntryComponentTest.setId("TestEntryID");
    measureEntryComponentTest.setResource(measureBundleTest);
    componentListTest.add(measureEntryComponentTest);
    responseBundleTest.setEntry(componentListTest);

    when(fhirDataProvider.searchBundleByTag(Constants.MainSystem, Constants.ReportDefinitionTag)).thenReturn(responseBundleTest);
    ApiConfig config = new ApiConfig();
    config.setMeasurePackages(new ArrayList<>());
    reportDefinitionControllerTest.setConfig(config);
    List<StoredMeasure> measures = reportDefinitionControllerTest.getMeasures(authMock.getAuthentication(), request);
    Assert.assertEquals( 1, measures.size());
    Assert.assertEquals("testMeasureTitle", measures.get(0).getName());
  }
}
