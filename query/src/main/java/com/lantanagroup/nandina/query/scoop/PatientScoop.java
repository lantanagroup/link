package com.lantanagroup.nandina.query.scoop;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.lantanagroup.nandina.query.PatientData;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.hapi.ctx.IValidationSupport;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class PatientScoop extends Scoop {
    protected FhirContext ctx = FhirContext.forR4();
    protected IParser jsonParser = ctx.newJsonParser();
    protected IParser xmlParser;
    protected IGenericClient targetFhirServer;
    protected IGenericClient nandinaFhirServer;
    protected Map<String, Patient> patientMap = new HashMap<>();
    protected IValidationSupport validationSupport;
    protected FHIRPathEngine fhirPathEngine;
    private final static String PATIENT_SEARCH_URL = "https://fhir.nandina.org/fhir/Patient?identifier=";

    public PatientScoop(IGenericClient targetFhirServer, IGenericClient nandinaFhirServer, List<String> patientIdList) throws Exception {
        this.targetFhirServer = targetFhirServer;
        this.nandinaFhirServer = nandinaFhirServer;
        patientData = loadPatientData(patientIdList);
    }

    public List<PatientData> loadPatientData(List<String> patientIdList) throws Exception {
        List<PatientData> patientDataList = new ArrayList<>();

        // first get the patients and store them in the patientMap
        patientIdList.forEach(patientId -> {
            try {
                String searchUrl = "Patient?identifier=" + patientId;
                Bundle response = this.nandinaFhirServer.search()
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
                logger.error("Unable to retrieve patient with identifier " + patientId + " from FHIR server " + this.nandinaFhirServer.getServerBase() + " due to authentication errors: \n" + ae.getResponseBody());
                ae.printStackTrace();
            } catch (Exception e) {
                logger.error("Unable to retrieve patient with identifier " + patientId + " from FHIR server " + this.nandinaFhirServer.getServerBase());
                e.printStackTrace();
            }
        });

        try {
            // loop through the patient ids to retrieve the patientData using each patient.
            patientIdList.parallelStream().forEach(id -> {
                PatientData patientData;
                try {
                    Patient patient = this.getPatientMap().get(id);
                    if (null != patient) {
                        patientData = new PatientData(this, patient, targetFhirServer.getFhirContext());
                        patientDataList.add(patientData);
                    } else {
                        logger.warn("Patient Id: " + id + " for patientData doesn't exist.");
                    }
                } catch (Exception e) {
                    logger.error("Error loading data for " + id, e);
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        logger.info("Patient Data List count: " + patientDataList.size());
        return patientDataList;
    }

    public Bundle rawSearch(String query) {
        return rawSearch(targetFhirServer, query);
    }
}
