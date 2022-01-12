package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.model.UserModel;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
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

  //Tests if the bundle is empty.
  @Test
  public void getUsersTestWithNoPractitioners(){

    setup();

    FhirDataProvider fhirDataProvider = new FhirDataProvider(clientTest);

    Bundle bundle = fhirDataProvider
            .searchPractitioner(tagSystemTest, tagValueTest);

    List<IBaseResource> bundles = FhirHelper.getAllPages(bundle, fhirDataProvider, ctxTest);
    Stream<UserModel> lst = bundles.parallelStream().map(practitioner -> new UserModel((Practitioner) practitioner));

    lst.collect(Collectors.toList());
  }

  //Tests when at least one practitioner is in the bundle.
  @Test
  public void getUsersTestWithPractitioners(){

    setup();

    FhirDataProvider fhirDataProvider = new FhirDataProvider(clientTest);

    Bundle bundle = fhirDataProvider
            .searchPractitioner(tagSystemTest, tagValueTest);

    bundle.addEntry().setResource(practitionerTest);

    List<IBaseResource> bundles = FhirHelper.getAllPages(bundle, fhirDataProvider, ctxTest);
    Stream<UserModel> lst = bundles.parallelStream().map(practitioner -> new UserModel((Practitioner) practitioner));

    lst.collect(Collectors.toList());
  }

  //This tests what happens when wrong values are put into searchPractitioner, it's supposed to fail.
  @Test
  public void getUsersNegativeTestWithPractitioners(){

    setup();

    FhirDataProvider fhirDataProvider = new FhirDataProvider(clientTest);

    Bundle bundle = fhirDataProvider
            .searchPractitioner("WrongValue1", "WrongValue2");

    bundle.addEntry().setResource(practitionerTest);

    List<IBaseResource> bundles = FhirHelper.getAllPages(bundle, fhirDataProvider, ctxTest);
    Stream<UserModel> lst = bundles.parallelStream().map(practitioner -> new UserModel((Practitioner) practitioner));

    lst.collect(Collectors.toList());
  }

  private void setup(){

    Given1 = new ArrayList<>();
    Given1.add(new StringType("Rosa"));
    Given1.add(new StringType("Emma"));
    Name1 = HumanNameSetup(Given1, "Smith");
    practitionerID1 = "_Practitioner:a2927697-0f9d-4240-a551-c16e4b9f5178_history1";
    practitionerTest = createPractitioner(practitionerID1, Name1);

    ctxTest = FhirContext.forR4();

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

  public Practitioner createPractitioner(String id, List Name) {
    Practitioner practitioner = new Practitioner();
    practitioner.setId(id);
    practitioner.setName(Name);
    return practitioner;
  }
}