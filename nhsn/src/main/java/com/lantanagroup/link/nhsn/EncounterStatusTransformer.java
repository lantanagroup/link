package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.IReportGenerationEvent;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



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
              previous.setUrl(Constants.OriginalEncounterStatus);
              Coding coding = new Coding();
              coding.setCode(patientEncounter.getStatus().toString());
              previous.setValue(coding);
              patientEncounter.setStatus(Encounter.EncounterStatus.FINISHED);
              patientEncounter.addExtension(previous);
              patientResource.setResource(patientEncounter);
            }
          }
        }
        fhirDataProvider.updateResource(patientBundle);
        fhirDataProvider.audit(context.getRequest(), context.getUser().getJwt(), FhirHelper.AuditEventTypes.Export, "Successfully transformed encounters with an end date to finished.");
      }
      catch (Exception ex) {
        logger.error("Exception is: " + ex.getMessage());
      }
    }
  }
}