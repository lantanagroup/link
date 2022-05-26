package com.lantanagroup.link.query.uscore.scoop;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.query.uscore.PatientData;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Getter
@Setter
@Component
public class PatientScoop extends Scoop {
  protected IGenericClient fhirQueryServer;

  @Autowired
  private ApplicationContext context;

  @Autowired
  private QueryConfig queryConfig;

  public void execute(List<PatientOfInterestModel> pois, List<String> resourceTypes) throws Exception {
    if (this.fhirQueryServer == null) {
      throw new Exception("No FHIR server to query");
    }

    this.patientData = this.loadPatientData(pois, resourceTypes);
  }

  private synchronized PatientData loadPatientData(Patient patient, List<String> resourceTypes) {
    if (patient == null) return null;

    try {
      PatientData patientData = new PatientData(this.getFhirQueryServer(), patient, this.queryConfig, resourceTypes);
      patientData.loadData();
      return patientData;
    } catch (Exception e) {
      logger.error("Error loading data for Patient with logical ID " + patient.getIdElement().getIdPart(), e);
    }

    return null;
  }

  public List<PatientData> loadPatientData(List<PatientOfInterestModel> patientsOfInterest, List<String> resourceTypes) {
    // first get the patients and store them in the patientMap
    Map<String, Patient> patientMap = new HashMap<>();
    patientsOfInterest.forEach(poi -> {
      try {
        if (poi.getReference() != null) {
          String id = poi.getReference();

          if (id.indexOf("/") > 0) {
            id = id.substring(id.indexOf("/") + 1);
          }

          Patient patient = this.fhirQueryServer.read()
                  .resource(Patient.class)
                  .withId(id)
                  .execute();
         patientMap.put(poi.getReference(), patient);
        } else if (poi.getIdentifier() != null) {
          String searchUrl = "Patient?identifier=" + poi.getIdentifier();
          Bundle response = this.fhirQueryServer.search()
                  .byUrl(searchUrl)
                  .returnBundle(Bundle.class)
                  .execute();
          if (response.getEntry().size() != 1) {
            logger.info("Did not find one Patient with identifier " + poi);
          } else {
            Patient patient = (Patient) response.getEntryFirstRep().getResource();
            patientMap.put(poi.getIdentifier(), patient);
          }
        }
      } catch (AuthenticationException ae) {
        logger.error("Unable to retrieve patient with identifier " + poi + " from FHIR server " + this.fhirQueryServer.getServerBase() + " due to authentication errors: \n" + ae.getResponseBody());
        ae.printStackTrace();
        throw new RuntimeException(ae);
      } catch (Exception e) {
        logger.error("Unable to retrieve patient with identifier " + poi + " from FHIR server " + this.fhirQueryServer.getServerBase());
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    });

    try {
      // loop through the patient ids to retrieve the patientData using each patient.
      List<Patient> patients = new ArrayList<>(patientMap.values());
      int threshold = queryConfig.getParallelPatients();
      logger.info(String.format("Throttling patient query load to " + threshold + " at a time"));
      ForkJoinPool forkJoinPool = new ForkJoinPool(threshold);

      List<PatientData> patientDataList = forkJoinPool.submit(() -> patients.parallelStream().map(patient -> {
        logger.debug(String.format("Beginning to load data for patient with logical ID %s", patient.getIdElement().getIdPart()));
        PatientData patientData = this.loadPatientData(patient, resourceTypes);
        return patientData;
      })).get().collect(Collectors.toList());
      logger.info("Patient Data List count: " + patientDataList.size());
      return patientDataList;
    } catch (Exception e) {
      logger.error(e.getMessage());
    }

    return new ArrayList<>();
  }
}
