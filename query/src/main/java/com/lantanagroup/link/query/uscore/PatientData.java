package com.lantanagroup.link.query.uscore;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.query.uscore.scoop.PatientScoop;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class PatientData {
  private static final Logger logger = LoggerFactory.getLogger(PatientData.class);

  private final Patient patient;
  private final String patientId;
  private final IGenericClient fhirQueryServer;
  private final USCoreConfig usCoreConfig;
  private List<Bundle> bundles = new ArrayList<>();

  public PatientData(IGenericClient fhirQueryServer, Patient patient, USCoreConfig usCoreConfig) {
    this.fhirQueryServer = fhirQueryServer;
    this.patient = patient;
    this.patientId = patient.getIdElement().getIdPart();
    this.usCoreConfig = usCoreConfig;
  }

  public void loadData() {
    if (this.usCoreConfig == null || this.usCoreConfig.getQueries() == null || this.usCoreConfig.getQueries().size() == 0) {
      logger.error("Not configured for US Core queries. Not querying for any patient data.");
      return;
    }

    List<String> queryString = this.usCoreConfig.getQueries().stream().map(query ->
            query.replace("{{patientId}}", this.patientId)
    ).collect(Collectors.toList());

    queryString.parallelStream().forEach(query -> {
      Bundle bundle = PatientScoop.rawSearch(this.fhirQueryServer, query);
      this.bundles.add(bundle);
    });
  }

  public Bundle getBundleTransaction() {
    Bundle bundle = new Bundle();
    bundle.setType(BundleType.TRANSACTION);
    bundle.setIdentifier(new Identifier().setValue(this.patientId));
    bundle.addEntry().setResource(this.patient).getRequest().setMethod(Bundle.HTTPVerb.PUT).setUrl("Patient/" + patient.getIdElement().getIdPart());

    for (Bundle next : this.bundles) {
      FhirHelper.addEntriesToBundle(next, bundle);
    }

    HashSet<String> extraResources = getExtraResourses(bundle);

    return bundle;
  }

  private HashSet<String> getExtraResourses(Bundle inBundle){

    HashSet<String> resourceReferences = new HashSet<>();

    for(int i = 0; i < inBundle.getEntry().size(); i++){

      if(this.usCoreConfig.getExtraResources().size() > 0){
        String resourseType = inBundle.getEntry().get(i).getResource().getResourceType().name();

        if(resourseType == "MedicationRequest" && this.usCoreConfig.getExtraResources().contains("Medication/{{medicationId}}")){
          MedicationRequest medicationRequest = (MedicationRequest) inBundle.getEntry().get(i).getResource();
          resourceReferences.add(medicationRequest.getMedicationReference().getReference());
        }

        if(resourseType == "Encounter" && this.usCoreConfig.getExtraResources().contains("Location/{{locationID}}")){
          Encounter encounter = (Encounter) inBundle.getEntry().get(i).getResource();

          for(int j = 0; j < encounter.getLocation().size(); j++){
            resourceReferences.add(encounter.getLocation().get(j).getLocation().getReference());
          }
        }
      }
    }

    return resourceReferences;
  }
}
