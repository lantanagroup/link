package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.*;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class EncounterStatusTransformer implements IReportGenerationDataEvent {
  private static final Logger logger = LoggerFactory.getLogger(EncounterStatusTransformer.class);

  @Autowired
  private FhirDataProvider fhirDataProvider;

  @Override
  public void execute(Bundle bundle) {
    logger.info("Called: " + EncounterStatusTransformer.class.getName());
    for(Bundle.BundleEntryComponent patientResource : bundle.getEntry()) {
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
  }

  @Override
  public void execute(List<DomainResource> data) throws RuntimeException{
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public void execute(Bundle data, ReportCriteria criteria, ReportContext context) {
    execute(data);
    fhirDataProvider.audit(context.getRequest(), context.getUser().getJwt(), FhirHelper.AuditEventTypes.Transformation, "Successfully transformed encounters with an end date to finished.");
    fhirDataProvider.updateResource(data);
  }

}
