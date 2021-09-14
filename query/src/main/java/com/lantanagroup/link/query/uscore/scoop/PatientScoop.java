package com.lantanagroup.link.query.uscore.scoop;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.query.uscore.PatientData;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.hapi.ctx.IValidationSupport;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Getter
@Setter
@Component
public class PatientScoop extends Scoop {
  protected FhirContext ctx = FhirContext.forR4();
  protected IParser jsonParser = ctx.newJsonParser();
  protected IParser xmlParser;
  protected IGenericClient fhirQueryServer;
  protected Map<String, Patient> patientMap = new HashMap<>();
  protected IValidationSupport validationSupport;
  protected FHIRPathEngine fhirPathEngine;

  @Autowired
  private ApplicationContext context;

  @Autowired
  private USCoreConfig usCoreConfig;

  public void execute(List<PatientOfInterestModel> pois) throws Exception {
    if (this.fhirQueryServer == null) {
      throw new Exception("No FHIR server to query");
    }

    this.patientData = this.loadPatientData(pois);
  }

  private synchronized PatientData loadPatientData(Patient patient) {
    if (patient == null) return null;

    try {
      PatientData patientData = new PatientData(this.getFhirQueryServer(), patient, this.usCoreConfig);
      patientData.loadData();
      return patientData;
    } catch (Exception e) {
      logger.error("Error loading data for Patient with logical ID " + patient.getIdElement().getIdPart(), e);
    }

    return null;
  }

  public List<PatientData> loadPatientData(List<PatientOfInterestModel> patientsOfInterest) {
    // first get the patients and store them in the patientMap
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
          this.patientMap.put(poi.getReference(), patient);
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
            this.patientMap.put(poi.getIdentifier(), patient);
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
      List<Patient> patients = new ArrayList<>(this.getPatientMap().values());

      // loop through the patient ids to retrieve the patientData using each patient.
      List<PatientData> patientDataList = patients.parallelStream().map(patient -> {
        logger.debug(String.format("Beginning to load data for patient with logical ID %s", patient.getIdElement().getIdPart()));
        PatientData patientData = this.loadPatientData(patient);
        return patientData;
      }).collect(Collectors.toList());

      logger.info("Patient Data List count: " + patientDataList.size());
      return patientDataList;
    } catch (Exception e) {
      logger.error(e.getMessage());
    }

    return new ArrayList<>();
  }
}
