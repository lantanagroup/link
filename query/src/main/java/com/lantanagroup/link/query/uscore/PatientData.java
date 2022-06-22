package com.lantanagroup.link.query.uscore;


import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.ResourceIdChanger;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.query.uscore.scoop.PatientScoop;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PatientData {
  private static final Logger logger = LoggerFactory.getLogger(PatientData.class);

  private final Patient patient;
  private final String patientId;
  private final IGenericClient fhirQueryServer;
  private final USCoreConfig usCoreConfig;
  // private final QueryConfig queryConfig;
  private List<String> resourceTypes;
  private List<Bundle> bundles = new ArrayList<>();

  public PatientData(IGenericClient fhirQueryServer, Patient patient, USCoreConfig usCoreConfig, List<String> resourceTypes) {
    this.fhirQueryServer = fhirQueryServer;
    this.patient = patient;
    this.patientId = patient.getIdElement().getIdPart();
    this.usCoreConfig = usCoreConfig;
    this.resourceTypes = resourceTypes;
  }

  public void loadData() {
    if (resourceTypes.size() == 0) {
      logger.error("Not querying for any patient data.");
      return;
    }

    List<String> queryString = resourceTypes.stream().map(query -> {
      String returnedQuery;
      if (query.equals("Observation")) {
        returnedQuery = query + "?category=laboratory&patient=Patient/" + this.patientId;
      } else {
        returnedQuery = query + "?patient=Patient/" + this.patientId;
      }
      return returnedQuery;
    }).collect(Collectors.toList());

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

    this.getAdditionalResources(bundle, ResourceIdChanger.findReferences(bundle));

    return bundle;
  }

  private void getAdditionalResources(Bundle bundle, List<Reference> resourceReferences){
    if (this.usCoreConfig.getOtherResourceTypes() != null) {
      for (Reference reference : resourceReferences) {
        String[] refParts = reference.getReference().split("/");
        List<String> otherResourceTypes = this.usCoreConfig.getOtherResourceTypes();
        if (otherResourceTypes.contains(refParts[0])) {
          Resource resource = (Resource) this.fhirQueryServer.read()
                  .resource(refParts[0])
                  .withId(refParts[1])
                  .execute();
          bundle.addEntry().setResource(resource);
        }
      }
    }
  }
}
