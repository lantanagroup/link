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
import org.springframework.beans.factory.annotation.Autowired;

public class EncounterStatusTransformer implements IReportGenerationEvent {
  private static final Logger logger = LoggerFactory.getLogger(EncounterStatusTransformer.class);

  @Autowired
  private FhirDataProvider fhirDataProvider;


  @Override
  public void execute(ReportCriteria reportCriteria, ReportContext context) {
    for (PatientOfInterestModel patientOfInterest : context.getPatientsOfInterest()) {
      logger.debug("Reviewing encounter status for patient " + patientOfInterest.getId());
      try {
        Bundle patientBundle = fhirDataProvider.getBundleById(context.getReportId() + "-" + patientOfInterest.getId().hashCode());
        for(Bundle.BundleEntryComponent patientResource : patientBundle.getEntry()) {
          if(patientResource.getResource().getResourceType().equals(ResourceType.Encounter)) {
            logger.debug("Reviewing encounter " + patientResource.getResource().getId() + " status");
            Encounter patientEncounter = (Encounter)patientResource.getResource();
            if(patientEncounter.getPeriod().hasEnd()) {
              Extension previous = new Extension();
              previous.setUrl(Constants.OriginalEncounterStatus);
              Coding coding = new Coding();
              coding.setCode(patientEncounter.getStatus().toString());
              previous.setValue(coding);
              patientEncounter.setStatus(Encounter.EncounterStatus.FINISHED);
              patientEncounter.addExtension(previous);
            }
          }
        }
        fhirDataProvider.updateResource(patientBundle);
        fhirDataProvider.audit(context.getRequest(), context.getUser().getJwt(), FhirHelper.AuditEventTypes.Transformation, "Successfully transformed encounters with an end date to finished.");
      }
      catch (Exception ex) {
        logger.error("Exception is: " + ex.getMessage());
      }
    }
  }
}
