package com.lantanagroup.link.query.uscore;

import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.query.BaseQuery;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.uscore.scoop.PatientScoop;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
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
      PatientScoop scoop = this.context.getBean(PatientScoop.class);
      scoop.setFhirQueryServer(this.getFhirQueryClient());
      scoop.execute(List.of(patientIdentifiers));

      List<PatientData> patientDatas = scoop.getPatientData();

      for (PatientData patientData : patientDatas) {
        Bundle patientBundle = patientData.getBundleTransaction();
        FhirHelper.addEntriesToBundle(patientBundle, bundle);
        bundle.setTotal(bundle.getEntry().size());
      }
    } catch (Exception ex) {
      logger.error("Error scooping data for patients: " + ex.getMessage());
      ex.printStackTrace();
    }

    return bundle;
  }
}
