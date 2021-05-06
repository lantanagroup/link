package com.lantanagroup.link.query;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.config.QueryConfig;
import com.lantanagroup.link.query.auth.HapiFhirAuthenticationInterceptor;
import com.lantanagroup.link.query.scoop.PatientScoop;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.List;

public class Query {
  private static final Logger logger = LoggerFactory.getLogger(Query.class);

  private QueryConfig config;

  private ApplicationContext context;
  
  public Query(ApplicationContext context) {
    this.context = context;
    this.config = context.getBean(QueryConfig.class);
  }
  
  public Bundle execute(String[] patientIdentifiers) throws ClassNotFoundException {
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

    if (Strings.isNotEmpty(this.config.getAuthClass())) {
      fhirQueryServer.registerInterceptor(new HapiFhirAuthenticationInterceptor(this.config, this.context));
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
