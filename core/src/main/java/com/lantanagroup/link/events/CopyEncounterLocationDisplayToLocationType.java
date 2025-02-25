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

import java.util.List;
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
  public void execute(TenantService tenantService, Bundle bundle, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      Resource resource = entry.getResource();
      if (!(resource instanceof Encounter)) {
        continue;
      }
      Encounter encounter = (Encounter) resource;
      for (Encounter.EncounterLocationComponent encounterLocation : encounter.getLocation()) {
        String reference = encounterLocation.getLocation().getReference();
        String display = encounterLocation.getLocation().getDisplay();
        String[] pieces = display.split(", ");
        String code = "";
        if(pieces.length >= 2){
          code = pieces[2];
        }
        if(!code.isEmpty()){
          List<Bundle.BundleEntryComponent> foundLocation = bundle.getEntry().stream().filter(e -> {
            Resource r = e.getResource();
            return r instanceof Location && r.getIdPart().equals(reference.substring(reference.indexOf("/") + 1));
          }).collect(Collectors.toList());

          if(!(foundLocation.isEmpty())) {
            Location location = (Location) foundLocation.get(0).getResource();

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
