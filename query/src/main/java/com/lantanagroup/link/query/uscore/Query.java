package com.lantanagroup.link.query.uscore;

import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.QueryResponse;
import com.lantanagroup.link.query.BaseQuery;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.uscore.scoop.PatientScoop;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class Query extends BaseQuery implements IQuery {
  private static final Logger logger = LoggerFactory.getLogger(Query.class);

  @Override
  public List<QueryResponse> execute(List<PatientOfInterestModel> patientsOfInterest) {
    List<QueryResponse> result;
    if (patientsOfInterest == null) {
      throw new IllegalArgumentException("patientsOfInterest");
    }

    List<QueryResponse> queryResponses = new ArrayList();

    if (patientsOfInterest.size() == 0) {
      result = queryResponses;
    } else {
      try {
        PatientScoop scoop = this.applicationContext.getBean(PatientScoop.class);
        scoop.setFhirQueryServer(this.getFhirQueryClient());
        scoop.execute(patientsOfInterest);

        List<PatientData> patientDatas = scoop.getPatientData();

        for (PatientData patientData : patientDatas) {
          Bundle patientBundle = patientData.getBundleTransaction();
          Optional<Bundle.BundleEntryComponent> patientEntry = patientBundle.getEntry().stream().filter(e -> e.getResource().getResourceType() == ResourceType.Patient).findFirst();

          if (patientEntry.isEmpty()) {
            logger.error("No Patient resource found in patient data bundle. Not adding them to the query responses.");
            continue;
          };

          Patient patient = (Patient) patientEntry.get().getResource();

          QueryResponse queryResponse = new QueryResponse(patient.getIdElement().getIdPart(), patientBundle);
          queryResponse.getBundle().setType(Bundle.BundleType.SEARCHSET);
          queryResponse.getBundle().setTotal(queryResponse.getBundle().getEntry().size());
          queryResponses.add(queryResponse);
        }
      } catch (Exception ex) {
        logger.error("Error scooping data for patients: " + ex.getMessage());
        ex.printStackTrace();
      }
      result = queryResponses;
    }

    return result;
  }
}
