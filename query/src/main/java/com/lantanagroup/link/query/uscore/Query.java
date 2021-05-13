package com.lantanagroup.link.query.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.query.BaseQuery;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.uscore.scoop.PatientScoop;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Query extends BaseQuery implements IQuery {
  private static final Logger logger = LoggerFactory.getLogger(Query.class);
  
  @Override
  public Bundle execute(String[] patientIdentifiers) {
    if (patientIdentifiers == null) {
      throw new IllegalArgumentException("patientIdentifiers");
    }

    if (patientIdentifiers.length == 0) {
      return new Bundle();
    }

    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.SEARCHSET);

    try {
      IGenericClient fhirQueryServer = this.getFhirQueryClient();
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
