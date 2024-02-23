package com.lantanagroup.link.events;

import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.IReportGenerationDataEvent;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.lantanagroup.link.Constants;

/**
 * Populates the Location.type with the first value from Location.identifier
 */
public class CopyLocationIdentifierToType implements IReportGenerationDataEvent {

  private static final Logger logger = LoggerFactory.getLogger(CopyLocationIdentifierToType.class);

  @Override
  public void execute(TenantService tenantService, Bundle bundle, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    //This is a specific transform to move data from an extension to the type of a Location resource for UMich
    //This must happen BEFORE ApplyConceptMaps as an event
    logger.info("Called: " + CopyLocationIdentifierToType.class.getName());
    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource().getResourceType().equals(ResourceType.Location)) {
        Location locationResource = (Location) entry.getResource();
        //Check for ConceptMap extension indicating that the identifier move to type and ConceptMapping has already been performed on this Location
        List<Coding> conceptMapExtensionCheck =
                locationResource.getType().stream().flatMap(t -> t.getCoding().stream()).filter(t ->
                                t.getExtension().stream().anyMatch(e -> e.getUrl().equals(Constants.ConceptMappingExtension)))
                        .collect(Collectors.toList());
        //If not already performed, continue
        if (conceptMapExtensionCheck.size() == 0) {
          List<Identifier> identifiers = locationResource.getIdentifier();
          List<CodeableConcept> types = locationResource.getType();
          if (identifiers.size() > 0) {
            for (Identifier identifier : identifiers) {
              String idValue = identifier.getValue();
              String idSystem = identifier.getSystem();

              List<CodeableConcept> existingType = types.stream()
                      .filter(t -> t.getCoding().stream().anyMatch(c -> c.getCode().equals(idValue) && c.getSystem().equals(idSystem)))
                      .collect(Collectors.toList());

              //Check for a full identifier (both system and value) and no existing type element with the values from identifier
              if (existingType.isEmpty() && idValue != null && idSystem != null) {

                //Add type to list of existing types
                types.add(FhirHelper.createCodeableConcept(idValue, idSystem));
              }
            }
          }
        }
      }
    }
  }

  @Override
  public void execute(TenantService tenantService, List<DomainResource> data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
  }
}
