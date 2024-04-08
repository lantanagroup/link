package com.lantanagroup.link.events;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.IReportGenerationDataEvent;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Changes the Encounter.status to "finished" if the Encounter has an end date
 */
public class EncounterStatusTransformer implements IReportGenerationDataEvent {
  private static final Logger logger = LoggerFactory.getLogger(EncounterStatusTransformer.class);

  @Override
  public void execute(TenantService tenantService, Bundle bundle, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    for (Bundle.BundleEntryComponent patientResource : bundle.getEntry()) {
      if (patientResource.getResource().getResourceType().equals(ResourceType.Encounter)) {
        Encounter patientEncounter = (Encounter) patientResource.getResource();

        if (patientEncounter.getPeriod().hasEnd() && patientEncounter.getStatus() != Encounter.EncounterStatus.FINISHED) {
          logger.debug("Updating Encounter {} status from {} to FINISHED", patientEncounter.getIdElement().getIdPart(), patientEncounter.getStatus());

          patientEncounter.getStatusElement().addExtension()
                  .setUrl(Constants.OriginalElementValueExtension)
                  .setValue(new CodeType(patientEncounter.getStatus().toCode()));
          patientEncounter.setStatus(Encounter.EncounterStatus.FINISHED);
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
