package com.lantanagroup.nandina.query;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.config.AuthConfigModes;
import com.lantanagroup.nandina.config.IQueryConfig;
import com.lantanagroup.nandina.hapi.HapiFhirAuthenticationInterceptor;
import com.lantanagroup.nandina.query.scoop.PatientScoop;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Query {
  private static final Logger logger = LoggerFactory.getLogger(Query.class);

  private IQueryConfig config;
  
  public Query(IQueryConfig config) {
    this.config = config;
  }
  
  public Bundle execute(String[] patientIdentifiers) {
    if (patientIdentifiers == null) {
      throw new IllegalArgumentException("patientIdentifiers");
    }

    if (patientIdentifiers.length == 0) {
      return new Bundle();
    }

    FhirContext ctx = FhirContext.forR4();
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.SEARCHSET);

    IGenericClient fhirQueryServer = ctx.newRestfulGenericClient(this.config.getFhirServerBase());

    if (this.config.getQueryAuth() != null && this.config.getQueryAuth().getAuthMode() != AuthConfigModes.None) {
      fhirQueryServer.registerInterceptor(new HapiFhirAuthenticationInterceptor(this.config.getQueryAuth()));
    }

    try {
      PatientScoop scoop = new PatientScoop(fhirQueryServer, List.of(patientIdentifiers));

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
