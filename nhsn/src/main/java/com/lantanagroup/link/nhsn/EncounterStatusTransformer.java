package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.IReportGenerationEvent;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiConfigEvents;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.QueryResponse;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;



public class EncounterStatusTransformer implements IReportGenerationEvent {
  private static final Logger logger = LoggerFactory.getLogger(EncounterStatusTransformer.class);

  @Autowired
  @Setter
  private ApiConfigEvents apiConfigEvents;

  @Override
  public void execute(ReportCriteria reportCriteria, ReportContext context, ApiConfig config, FhirDataProvider fhirDataProvider) {

    for (PatientOfInterestModel patientOfInterest : context.getPatientsOfInterest()) {
      logger.info("Patient is: " + patientOfInterest.getId());
      try {
        Bundle patientBundle = fhirDataProvider.getBundleById(context.getReportId() + "-" + patientOfInterest.getId().hashCode());
        for(Bundle.BundleEntryComponent patientResource : patientBundle.getEntry()) {
          if(patientResource.getResource().getResourceType().equals(ResourceType.Encounter)) {
            logger.info("Patient encounter is: " + patientResource.getResource().getId());
            Encounter patientEncounter = (Encounter)patientResource.getResource();
            if(patientEncounter.getPeriod().hasEnd()) {
              patientEncounter.setStatus(Encounter.EncounterStatus.FINISHED);
            }
          }
        }

      }
      catch (Exception ex) {
        logger.error("Exception is: " + ex.getMessage());
      }

    }

    /*List<String> encounters = null;
    try {
      Method eventMethodInvoked = ApiConfigEvents.class.getMethod("getAfterPatientDataQuery");
      encounters = (List<String>) eventMethodInvoked.invoke(apiConfigEvents);
    } catch (Exception ex) {
      logger.error(String.format("Error in triggerEvent for event AfterPatientDataQuery: ") + ex.getMessage());
    }
    if(encounters != null) {
      for (String encounter : encounters) {

      }
    }*/
  }
}