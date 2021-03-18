package com.lantanagroup.nandina.query.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.config.AuthConfigModes;
import com.lantanagroup.nandina.hapi.HapiFhirAuthenticationInterceptor;
import com.lantanagroup.nandina.query.PatientData;
import com.lantanagroup.nandina.config.IQueryConfig;
import com.lantanagroup.nandina.query.config.QueryConfig;
import com.lantanagroup.nandina.query.scoop.PatientScoop;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class QueryController {
  private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

  @Autowired
  private QueryConfig springConfig;

  private IQueryConfig config;

  private IQueryConfig getConfig() {
    if (this.springConfig != null) {
      return this.springConfig;
    }

    return this.config;
  }

  public QueryController() {}

  public static Bundle getData(IQueryConfig config, String[] patientIdentifiers) {
    QueryController controller = new QueryController();
    controller.config = config;
    return controller.getData(patientIdentifiers);
  }

  @GetMapping(value = "/api/data", produces = {"application/json", "application/fhir+json", "application/xml", "application/fhir+xml"})
  public @ResponseBody Bundle getData(String[] patientIdentifier) {
    if (patientIdentifier == null || patientIdentifier.length == 0) {
      throw new IllegalArgumentException("patientIdentifier");
    }

    FhirContext ctx = FhirContext.forR4();
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.SEARCHSET);

    IGenericClient fhirQueryServer = ctx.newRestfulGenericClient(this.getConfig().getFhirServerBase());

    if (this.getConfig().getQueryAuth() != null && this.getConfig().getQueryAuth().getAuthMode() != AuthConfigModes.None) {
      fhirQueryServer.registerInterceptor(new HapiFhirAuthenticationInterceptor(this.getConfig().getQueryAuth()));
    }

    try {
      PatientScoop scoop = new PatientScoop(fhirQueryServer, List.of(patientIdentifier));

      for (PatientData patientData : scoop.getPatientData()) {
        Bundle next = patientData.getBundleTransaction();
        next.getEntry().forEach(bundle::addEntry);
        bundle.setTotal(bundle.getEntry().size());
      }
    } catch (Exception ex) {
      logger.error("Error scooping data for patients: " + ex.getMessage());
      ex.printStackTrace();
    }

    return bundle;
  }
}
