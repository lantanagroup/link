package com.lantanagroup.link.query.uscore;

import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.model.PatientOfInterestModel;
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
  public Bundle execute(List<PatientOfInterestModel> patientsOfInterest) {
    if (patientsOfInterest == null) {
      throw new IllegalArgumentException("patientsOfInterest");
    }

    if (patientsOfInterest.size() == 0) {
      return new Bundle();
    }

    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.SEARCHSET);

    try {
      PatientScoop scoop = this.applicationContext.getBean(PatientScoop.class);
      scoop.setFhirQueryServer(this.getFhirQueryClient());
      scoop.execute(patientsOfInterest);

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
