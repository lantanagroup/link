package com.lantanagroup.link.nhsn;

import com.auth0.jwt.JWT;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.IReportGenerationEvent;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiConfigEvents;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.QueryResponse;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import lombok.Setter;
import org.apache.catalina.connector.Request;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;



public class EncounterStatusTransformer implements IReportGenerationEvent {
  private static final Logger logger = LoggerFactory.getLogger(EncounterStatusTransformer.class);

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
              Extension previous = new Extension();
              previous.setValue(patientEncounter.getClass_());
              patientEncounter.setStatus(Encounter.EncounterStatus.FINISHED);
              patientResource.addExtension(previous);
              patientResource.setResource(patientEncounter);
              //fhirDataProvider.audit(new Request(), new JWT(), FhirHelper.AuditEventTypes.Export, "Recording transformation of encounter status.");
            }
          }
        }
        fhirDataProvider.updateResource(patientBundle);
      }
      catch (Exception ex) {
        logger.error("Exception is: " + ex.getMessage());
      }
    }
  }
}