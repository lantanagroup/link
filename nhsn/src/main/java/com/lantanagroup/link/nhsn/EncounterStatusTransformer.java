package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.IReportGenerationDataEvent;
import com.lantanagroup.link.TenantService;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EncounterStatusTransformer implements IReportGenerationDataEvent {
  private static final Logger logger = LoggerFactory.getLogger(EncounterStatusTransformer.class);

  @Override
  public void execute(TenantService tenantService, Bundle bundle, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    logger.info("Called: " + EncounterStatusTransformer.class.getName());
    for (Bundle.BundleEntryComponent patientResource : bundle.getEntry()) {
      if (patientResource.getResource().getResourceType().equals(ResourceType.Encounter)) {
        // logger.debug("Reviewing encounter " + patientResource.getResource().getId() + " status");
        Encounter patientEncounter = (Encounter) patientResource.getResource();
        if (patientEncounter.getPeriod().hasEnd()) {
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

//    fhirDataProvider.audit(context.getRequest(), context.getUser().getJwt(), FhirHelper.AuditEventTypes.Transformation, "Successfully transformed encounters with an end date to finished.");
//    fhirDataProvider.updateResource(bundle);
  }

  @Override
  public void execute(TenantService tenantService, List<DomainResource> data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {

  }
}
