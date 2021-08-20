package com.lantanagroup.link.query.uscore;


import ca.uhn.fhir.context.FhirContext;
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
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Component
public class PatientData {
  private static final Logger logger = LoggerFactory.getLogger(PatientData.class);

  private FhirContext ctx;
  private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  private Date dateCollected;
  private Patient patient;
  private String patientId;
  private Encounter primaryEncounter;
  private List<Bundle> bundles = new ArrayList<>();
  private CodeableConcept primaryDx = null;
  private PatientScoop patientScoop;

  public Patient getPatient() {
    return this.patient;
  }

  public void setPatient(Patient patient) {
    this.patient = patient;
    this.patientId = patient.getIdElement().getIdPart();
  }

  @Autowired
  private USCoreConfig usCoreConfig;

  public PatientData() {
  }

  public void loadData() {
    List<String> queryString = this.usCoreConfig.getQueries().stream().map(query ->
            query.replace("{{patientId}}", this.patientId)
    ).collect(Collectors.toList());

    queryString.parallelStream().forEach(query -> {
      Bundle bundle = this.patientScoop.rawSearch(query);
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

    return bundle;
  }
}
