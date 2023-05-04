package com.lantanagroup.link.events;

import com.lantanagroup.link.FhirScanner;
import com.lantanagroup.link.IReportGenerationDataEvent;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Moves any contained resources from resources retrieved from the EHR to the top-lavel of patient data
 */
public class ContainedResourceCleanup implements IReportGenerationDataEvent {
  private static final Logger logger = LoggerFactory.getLogger(ContainedResourceCleanup.class);
  private int fixCount = 0;

  @Override
  public void execute(TenantService tenantService, Bundle data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    int totalEntries = data.getEntry().size();
    for (int i = 0; i < totalEntries; i++) {
      DomainResource resource = (DomainResource) data.getEntry().get(i).getResource();

      if (resource.getContained().size() > 0) {
        resource.getContained().forEach(contained -> {
          DomainResource clone = (DomainResource) contained.copy();
          String newId = UUID.randomUUID().toString();
          clone.setId(newId);

          data.addEntry()
                  .setResource(clone);

          FhirScanner.findReferences(resource).forEach(ref -> {
            if (ref.getReference() != null && ref.getReference().equals(contained.getIdElement().getIdPart())) {
              ref.setReference(contained.getResourceType().toString() + "/" + newId);
            }
          });

          this.fixCount++;
        });

        resource.getContained().clear();
      }

      if (this.fixCount > 0) {
        logger.info("Moved {} contained resources to top level of patient bundle", this.fixCount);
      }
    }
  }

  @Override
  public void execute(TenantService tenantService, List<DomainResource> data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {

  }
}
