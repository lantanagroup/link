package com.lantanagroup.link.query.uscore.scoop;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
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

  public void execute(List<String> patientIds) throws Exception {
    if (this.fhirQueryServer == null) {
      throw new Exception("No FHIR server to query");
    }

    this.patientData = this.loadPatientData(patientIds);
  }

  private synchronized PatientData loadPatientData(Patient patient) {
    if (patient == null) return null;

    try {
      PatientData patientData = this.context.getBean(PatientData.class);
      patientData.setPatientScoop(this);
      patientData.setPatient(patient);
      patientData.setCtx(this.fhirQueryServer.getFhirContext());
      patientData.loadData();
      return patientData;
    } catch (Exception e) {
      logger.error("Error loading data for Patient with logical ID " + patient.getIdElement().getIdPart(), e);
    }

    return null;
  }

  public List<PatientData> loadPatientData(List<String> patientIdList) {
    Collection<PatientData> patientDataList = Collections.synchronizedCollection(new ArrayList<>());

    // first get the patients and store them in the patientMap
    patientIdList.forEach(patientId -> {
      try {
        String searchUrl = "Patient?identifier=" + patientId;
        Bundle response = this.fhirQueryServer.search()
                .byUrl(searchUrl)
                .returnBundle(Bundle.class)
                .execute();
        if (response.getEntry().size() != 1) {
          logger.info("Did not find one Patient with identifier " + patientId);
        } else {
          Patient patient = (Patient) response.getEntryFirstRep().getResource();
          this.patientMap.put(patientId, patient);
        }
      } catch (AuthenticationException ae) {
                logger.error("Unable to retrieve patient with identifier " + patientId + " from FHIR server " + this.fhirQueryServer.getServerBase() + " due to authentication errors: \n" + ae.getResponseBody());
                ae.printStackTrace();
                throw new RuntimeException(ae);
            } catch (Exception e) {
                logger.error("Unable to retrieve patient with identifier " + patientId + " from FHIR server " + this.fhirQueryServer.getServerBase());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });

        try {
          List<Patient> patients = new ArrayList<>(this.getPatientMap().values());

          // loop through the patient ids to retrieve the patientData using each patient.
          patients.parallelStream().forEach(patient -> {
            PatientData patientData = this.loadPatientData(patient);
            patientDataList.add(patientData);
          });
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        logger.info("Patient Data List count: " + patientDataList.size());
        return new ArrayList<>(patientDataList);
    }

    public Bundle rawSearch(String query) {
        return rawSearch(this.fhirQueryServer, query);
    }
}
