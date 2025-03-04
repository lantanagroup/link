package com.lantanagroup.link.events;

import com.lantanagroup.link.ApplyConceptMaps;
import com.lantanagroup.link.IReportGenerationDataEvent;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CopyEncounterLocationDisplayToLocationType implements IReportGenerationDataEvent {
  private static final Logger logger = LoggerFactory.getLogger(CopyEncounterLocationDisplayToLocationType.class);

  private static boolean existsInType(Location location, Coding coding) {
    return location.getType().stream()
            .flatMap(type -> type.getCoding().stream())
            .anyMatch(c -> c.is(coding.getSystem(), coding.getCode())
                    || ApplyConceptMaps.isMapped(c, coding.getSystem(), coding.getCode()));
  }

  private static void copyToType(Location location, Coding coding) {
    location.addType().addCoding(coding);
  }

  @Override
  public void execute(TenantService tenantService, Bundle bundle, ReportCriteria criteria, ReportContext context,
                      ReportContext.MeasureContext measureContext) {

    Map<String, Location> locations = new HashMap<>();
    for(Bundle.BundleEntryComponent entry : bundle.getEntry()){
      Resource resource = entry.getResource();
      if(resource instanceof Location) locations.put(resource.getIdPart(), (Location) resource);
    }

    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      Resource resource = entry.getResource();
      if (!(resource instanceof Encounter)) {
        continue;
      }
      Encounter encounter = (Encounter) resource;
      for (Encounter.EncounterLocationComponent encounterLocation : encounter.getLocation()) {
        if (encounterLocation.getLocation() == null || encounterLocation.getLocation().isEmpty()) {
          logger.debug("Encounter location reference is null for encounter {}", encounter.getId());
          continue;
        }
        String reference = encounterLocation.getLocation().getReference();
        String display = encounterLocation.getLocation().getDisplay();
        if (reference == null || display == null) {
          logger.debug("Encounter location reference or display is null for encounter {}", encounter.getId());
          continue;
        }
        String[] pieces = display.split(", ");
        String code = "";
        if(pieces.length >= 3) {
          code = pieces[2];
        } else {
          logger.debug("Display string '{}' doesn't have a third component for code extraction", display);
        }
        if(!code.isEmpty()){
          String locationReference = encounterLocation.getLocation().getReference();
          String locationID = locationReference.substring(locationReference.indexOf("/") + 1);
          Location location = locations.getOrDefault(locationID, null);

          if(location != null) {
            Coding coding = new Coding();
            coding.setCode(code);
            coding.setSystem(Constants.EncounterLocationDisplayCodeSystem);

            if (!existsInType(location, coding)) copyToType(location, coding);
          }
        }

      }
    }
  }

  @Override
  public void execute(TenantService tenantService, List<DomainResource> data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
  }
}
