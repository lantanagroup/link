package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.config.api.ApiConfig;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;


public class BaseController {

  @Setter
  protected FhirContext ctx = FhirContextProvider.getFhirContext();

  @Setter
  @Autowired
  protected ApiConfig config;

}
