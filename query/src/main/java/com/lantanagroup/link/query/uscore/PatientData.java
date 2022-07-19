package com.lantanagroup.link.query.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.ResourceIdChanger;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.config.query.USCoreQueryParametersResourceConfig;
import com.lantanagroup.link.config.query.USCoreQueryParametersResourceParameterConfig;
import com.lantanagroup.link.query.uscore.scoop.PatientScoop;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
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

  public void loadData(String measureId) {
    if (resourceTypes.size() == 0) {
      logger.error("Not querying for any patient data.");
      return;
    }

    //Loop through resource types specified. If observation, use config to add individual category queries
    List<String> queryString = new ArrayList<>();
    for (String resource: this.resourceTypes) {
      if(resource.equals("Observation")) {

        HashMap<String, List<USCoreQueryParametersResourceConfig>> queryParameters = this.usCoreConfig.getQueryParameters();

        //check if queryParameters exist in config, if not just load patient without observations
        if(queryParameters != null && !queryParameters.isEmpty()) {
          //this was written in a way that if the resource equals check was removed, it would work for other resource types
          this.usCoreConfig.getQueryParameters().get(measureId).stream().forEach(queryParams -> {
            for (USCoreQueryParametersResourceParameterConfig param: queryParams.getParameters()) {
              for (String paramValue: param.getValues()) {
                queryString.add(queryParams.getResourceType() + "?" + param.getName() + "=" + paramValue + "&patient=Patient/" + this.patientId);
              }
            }
          });
        }
        else {
          logger.warn("No observations found in US Core Config for %s, loading patient data without observations.", Helper.encodeLogging(measureId));
          queryString.add(resource + "?patient=Patient/" + this.patientId);
        }

      }
      else {
        queryString.add(resource + "?patient=Patient/" + this.patientId);
      }
    }

    if(!queryString.isEmpty()) {
      try {
        queryString.parallelStream().forEach(query -> {
          Bundle bundle = PatientScoop.rawSearch(this.fhirQueryServer, query);
          this.bundles.add(bundle);
        });
      }
      catch(Exception ex) {
        logger.error("Error while parallel processing patient data queries: %s", Helper.encodeLogging(ex.getMessage()));
      }
    }
    else{
      logger.warn("No queries generated based on resource types and configuration");
    }

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
      HashMap<String, List<String>> resourcesToGet = new HashMap<>();

      for (Reference reference : resourceReferences) {
        if (!reference.hasReference()) {
          continue;
        }

        String[] refParts = reference.getReference().split("/");
        List<String> otherResourceTypes = this.usCoreConfig.getOtherResourceTypes();
        if (otherResourceTypes.contains(refParts[0])) {
          if (!resourcesToGet.containsKey(refParts[0])) {
            resourcesToGet.put(refParts[0], new ArrayList<>());
          }

          List<String> resourceIds = resourcesToGet.get(refParts[0]);

          if (!resourceIds.contains(refParts[1])) {
            resourceIds.add(refParts[1]);
          }
        }
      }

      resourcesToGet.keySet().stream().forEach(resourceType -> {
        resourcesToGet.get(resourceType).parallelStream().forEach(resourceId -> {
          Resource resource = (Resource) this.fhirQueryServer.read()
                  .resource(resourceType)
                  .withId(resourceId)
                  .execute();
          bundle.addEntry().setResource(resource);
        });
      });
    }
  }
}
