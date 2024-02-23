package com.lantanagroup.link.events;

import com.lantanagroup.link.ApplyConceptMaps;
import com.lantanagroup.link.IReportGenerationDataEvent;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CopyLocationIdentifierToType implements IReportGenerationDataEvent {
  private static final Logger logger = LoggerFactory.getLogger(CopyLocationIdentifierToType.class);

  private static boolean existsInType(Location location, Identifier identifier) {
    return location.getType().stream()
            .flatMap(type -> type.getCoding().stream())
            .anyMatch(coding -> coding.is(identifier.getSystem(), identifier.getValue())
                    || ApplyConceptMaps.isMapped(coding, identifier.getSystem(), identifier.getValue()));
  }

  private static void copyToType(Location location, Identifier identifier) {
    location.addType().addCoding()
            .setSystem(identifier.getSystem())
            .setCode(identifier.getValue());
  }

  @Override
  public void execute(TenantService tenantService, Bundle bundle, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      Resource resource = entry.getResource();
      if (!(resource instanceof Location)) {
        continue;
      }
      Location location = (Location) resource;
      for (Identifier identifier : location.getIdentifier()) {
        if (identifier.hasSystem() && !existsInType(location, identifier)) {
          copyToType(location, identifier);
        }
      }
    }
  }

  @Override
  public void execute(TenantService tenantService, List<DomainResource> data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
  }
}
