package com.lantanagroup.link.events;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirScanner;
import com.lantanagroup.link.IReportGenerationDataEvent;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Moves any contained resources from resources retrieved from the EHR to the top-lavel of patient data
 */
public class ContainedResourceCleanup implements IReportGenerationDataEvent {
  private static final Logger logger = LoggerFactory.getLogger(ContainedResourceCleanup.class);

  @Override
  public void execute(TenantService tenantService, Bundle data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    int fixCount = 0;
    int totalEntries = data.getEntry().size();
    for (int i = 0; i < totalEntries; i++) {
      DomainResource resource = (DomainResource) data.getEntry().get(i).getResource();

      if (resource.getContained().size() > 0) {
        for (Resource contained : resource.getContained()) {
          DomainResource clone = (DomainResource) contained.copy();
          String newId = UUID.randomUUID().toString();
          clone.setId(newId);

          //Adding in when this resource was "created" from the contained section
          clone.getMeta().addExtension(Constants.ReceivedDateExtensionUrl, DateTimeType.now());

          data.addEntry()
                  .setResource(clone);

          FhirScanner.findReferences(resource).forEach(ref -> {
            if (ref.getReference() != null && ref.getReference().equals(contained.getIdElement().getIdPart())) {
              ref.setReference(contained.getResourceType().toString() + "/" + newId);
            }
          });

          fixCount++;
        }

        resource.getContained().clear();
      }
    }

    if (fixCount > 0) {
      logger.info("Moved {} contained resources to top level of patient bundle", fixCount);
    }
  }

  @Override
  public void execute(TenantService tenantService, List<DomainResource> data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {

  }
}
