package com.lantanagroup.nandina.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.api.config.ApiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

public class BaseController {
  protected FhirContext ctx = FhirContext.forR4();
  @Autowired
  private ApiConfig nandinaConfig;

  BaseController() {
    this.ctx.getRestfulClientFactory().setSocketTimeout(200 * 5000);
  }

  protected IGenericClient getFhirStoreClient(Authentication authentication, HttpServletRequest request) throws Exception {
    String fhirBase = nandinaConfig.getFhirServerStore();
    IGenericClient fhirClient = this.ctx.newRestfulGenericClient(fhirBase);
    return fhirClient;
  }
}
