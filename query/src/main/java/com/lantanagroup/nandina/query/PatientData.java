package com.lantanagroup.nandina.query;


import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.query.scoop.EncounterScoop;
import com.lantanagroup.nandina.query.scoop.PatientScoop;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor
public class PatientData {

  private static final Logger logger = LoggerFactory.getLogger(PatientData.class);
  private static FhirContext ctx;
  private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  private Date dateCollected;
  private Patient patient;
  private Encounter primaryEncounter;
  private Bundle encounters;
  private Bundle conditions;
  private Bundle meds;
  private Bundle labResults;
  private Bundle allergies;
  private Bundle procedures;
  private CodeableConcept primaryDx = null;
  private List<String> queryConstants =
          Arrays.asList("Encounter?patient=Patient/", "Condition?patient=Patient/", "MedicationRequest?patient=Patient/", "Observation?patient=Patient/", "AllergyIntolerance?patient=Patient/", "Procedure?patient=Patient/");

  public PatientData(PatientScoop patientScoop, Patient patient, FhirContext fhirContext) {
    this.patient = patient;
    ctx = fhirContext;
    List<String> queryString = new ArrayList<>();
    queryConstants.parallelStream().forEach(query -> {
      if (query.contains("Observation")) {
        queryString.add(query + patient.getIdElement().getIdPart() + "&category=http://terminology.hl7.org/CodeSystem/observation-category|laboratory");
      } else {
        queryString.add(query + patient.getIdElement().getIdPart());
      }
    });

    queryString.parallelStream().forEach(query -> {
      if (query.contains("Encounter")) {
        encounters = patientScoop.rawSearch(query);
      } else if (query.contains("Condition")) {
        conditions = patientScoop.rawSearch(query);
      } else if (query.contains("MedicationRequest")) {
        meds = patientScoop.rawSearch(query);
      } else if (query.contains("Observation")) {
        labResults = patientScoop.rawSearch(query);
      } else if (query.contains("AllergyIntolerance")) {
        allergies = patientScoop.rawSearch(query);
      } else if (query.contains("Procedure")) {
        procedures = patientScoop.rawSearch(query);
      }
    });
  }

  public PatientData(EncounterScoop encounterScoop, Patient pat, FhirContext fhirContext) {
    patient = pat;
    ctx = fhirContext;
    dateCollected = new Date();
    Map<Patient, Encounter> patEncMap = encounterScoop.getPatientEncounterMap();
    primaryEncounter = patEncMap.get(patient);
    List<String> queryString = new ArrayList<>();
    queryConstants.parallelStream().forEach(query -> {
      if (query.contains("Observation")) {
        queryString.add(query + pat.getIdElement().getIdPart() + "&category=http://terminology.hl7.org/CodeSystem/observation-category|laboratory");
      } else {
        queryString.add(query + pat.getIdElement().getIdPart());
      }
    });

    queryString.parallelStream().forEach(query -> {
      if (query.contains("Encounter")) {
        encounters = encounterScoop.rawSearch(query);
      } else if (query.contains("Condition")) {
        conditions = encounterScoop.rawSearch(query);
      } else if (query.contains("MedicationRequest")) {
        meds = encounterScoop.rawSearch(query);
      } else if (query.contains("Observation")) {
        labResults = encounterScoop.rawSearch(query);
      } else if (query.contains("AllergyIntolerance")) {
        allergies = encounterScoop.rawSearch(query);
      } else if (query.contains("Procedure")) {
        procedures = encounterScoop.rawSearch(query);
      }
    });
  }


  public Bundle getBundleTransaction() {
    Bundle b = new Bundle();
    b.setType(BundleType.TRANSACTION);
    b.addEntry().setResource(patient).getRequest().setMethod(Bundle.HTTPVerb.PUT).setUrl("Patient/" + patient.getIdElement().getIdPart());
    addEntryToBundle(encounters, b);
    addEntryToBundle(conditions, b);
    addEntryToBundle(meds, b);
    addEntryToBundle(labResults, b);
    addEntryToBundle(allergies, b);
    return b;
  }

  private void addEntryToBundle(Bundle source, Bundle destination) {
    source.getEntry().forEach(entry -> {
      destination.addEntry().setResource(entry
              .getResource())
              .getRequest()
              .setMethod(Bundle.HTTPVerb.PUT)
              .setUrl(entry.getResource().getResourceType().toString() + "/" + entry.getResource().getIdElement().getIdPart());
    });
  }

  public Bundle getBundle() {
    Bundle b = new Bundle();
    b.setType(BundleType.COLLECTION);
    b.addEntry().setResource(patient);
    b.addEntry().setResource(encounters);
    b.addEntry().setResource(conditions);
    b.addEntry().setResource(meds);
    b.addEntry().setResource(labResults);
    b.addEntry().setResource(allergies);
    return b;
  }

  public String getBundleXml() {
	  return ctx.newXmlParser().encodeResourceToString(getBundle());
  }

  /**
   * The setPrimaryDx() method is the only setter here,
   * since the primary diagnosis is not known until filters are run.
   * If not for that, this class could be a read-only final class.
   *
   * @param primaryDx
   */
  public void setPrimaryDx(CodeableConcept primaryDx) {
    this.primaryDx = primaryDx;
  }

}