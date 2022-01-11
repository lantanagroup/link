package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.FhirDataProvider;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class UserControllerTest extends BaseController{

  private String tagSystemTest = "https://nhsnlink.org";
  private String tagValueTest = "link-user";

  @Test
  public void getUsers() {

    FhirDataProvider fhirDataProvider = mock(FhirDataProvider.class);
    Bundle bundle = fhirDataProvider
            .searchPractitioner(tagSystemTest, tagValueTest);
  }
}