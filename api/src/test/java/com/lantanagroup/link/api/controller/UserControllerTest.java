package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.mock.AuthMockInfo;
import com.lantanagroup.link.mock.MockHelper;
import com.lantanagroup.link.model.UserModel;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserControllerTest extends BaseController{

  private String tagSystemTest = "https://nhsnlink.org";
  private String tagValueTest = "link-user";
  private FhirContext ctxTest;
  private IGenericClient clientTest;
  private IUntypedQuery<IBaseBundle> untypedQueryTest;
  private IQuery<IBaseBundle> IBundleQueryTest;
  private IQuery<Bundle> bundleQueryTest;
  private Bundle responseBundleTest;
  private Practitioner practitionerTest;
  private List<HumanName> Name1;
  private List<StringType> Given1;
  private String practitionerID1;

  //Test 1 user is returned
  @Test
  public void getUsersTest() throws Exception {

    setup();
    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    AuthMockInfo authMock = MockHelper.mockAuth(fhirStoreClient);
    HttpServletRequest request = mock(HttpServletRequest.class);
    UserController userController = new UserController();
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    userController.setFhirStoreProvider(fhirDataProvider);
    userController.setConfig(new ApiConfig());
    Bundle bundle = new Bundle();
    bundle.addEntry().setResource(practitionerTest);
    when(fhirDataProvider.searchPractitioner(anyString(), anyString())).thenReturn(bundle);

    List<UserModel> users = userController.getUsers(authMock.getAuthentication(), request);
    Assert.assertEquals(1, users.size());
  }


  //Test 0 users are returned
  @Test
  public void getZeroUsersTest() throws Exception {

    setup();
    IGenericClient fhirStoreClient = mock(IGenericClient.class);
    AuthMockInfo authMock = MockHelper.mockAuth(fhirStoreClient);
    HttpServletRequest request = mock(HttpServletRequest.class);
    UserController userController = new UserController();
    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    userController.setFhirStoreProvider(fhirDataProvider);
    userController.setConfig(new ApiConfig());
    Bundle bundle = new Bundle();
    when(fhirDataProvider.searchPractitioner(anyString(), anyString())).thenReturn(bundle);

    List<UserModel> users = userController.getUsers(authMock.getAuthentication(), request);
    Assert.assertEquals(0, users.size());
  }


  private void setup(){

    Given1 = new ArrayList<>();
    Given1.add(new StringType("Rosa"));
    Given1.add(new StringType("Emma"));
    Name1 = HumanNameSetup(Given1, "Smith");
    practitionerID1 = "_Practitioner:a2927697-0f9d-4240-a551-c16e4b9f5178_history1";
    practitionerTest = createPractitioner(practitionerID1, Name1);

    ctxTest = FhirContextProvider.getFhirContext();

    clientTest = mock(IGenericClient.class);
    untypedQueryTest = mock(IUntypedQuery.class);
    IBundleQueryTest = mock(IQuery.class);
    bundleQueryTest = mock(IQuery.class);
    responseBundleTest = new Bundle();

    when(clientTest.search()).thenReturn(untypedQueryTest);
    when(untypedQueryTest.forResource(Practitioner.class)).thenReturn(IBundleQueryTest);
    when(IBundleQueryTest.withTag(tagSystemTest, tagValueTest)).thenReturn(IBundleQueryTest);
    when(IBundleQueryTest.returnBundle(Bundle.class)).thenReturn(bundleQueryTest);
    when(bundleQueryTest.cacheControl(any(CacheControlDirective.class))).thenReturn(bundleQueryTest);
    when(bundleQueryTest.execute()).thenReturn(responseBundleTest);
  }

  public List<HumanName> HumanNameSetup(List<StringType> givenNames, String lastName){
    HumanName humanName = new HumanName();
    List<HumanName> Name = new ArrayList<>();

    Name.add(humanName);
    Name.get(0).setFamily(lastName);
    Name.get(0).setGiven(givenNames);
    return Name;
  }

  public Practitioner createPractitioner(String id, List<HumanName> Name) {
    Practitioner practitioner = new Practitioner();
    practitioner.setId(id);
    practitioner.setName(Name);
    return practitioner;
  }
}
