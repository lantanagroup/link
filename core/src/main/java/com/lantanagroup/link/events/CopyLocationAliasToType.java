package com.lantanagroup.link.events;

import com.lantanagroup.link.Constants;
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


public class CopyLocationAliasToType implements IReportGenerationDataEvent {
  private static final Logger logger = LoggerFactory.getLogger(CopyLocationAliasToType.class);

  @Override
  public void execute(TenantService tenantService, Bundle bundle, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    //This is a specific transform to move data from a Location's alias to its type
    //This must happen BEFORE ApplyConceptMaps as an event
    logger.info("Called: " + CopyLocationAliasToType.class.getName());
    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource().getResourceType().equals(ResourceType.Location)) {
        Location locationResource = (Location) entry.getResource();
        //Check for ConceptMap extension indicating that the alias move to type and ConceptMapping has already been performed on this Location
        List<CodeableConcept> conceptMapExtensionCheck =
                locationResource.getType().stream().filter(t ->
                                t.getCoding().stream().anyMatch(c ->
                                        c.getExtension().stream().anyMatch(e ->
                                                e.getUrl().equals(Constants.ConceptMappingExtension))))
                        .collect(Collectors.toList());

        //If not already performed, continue
        if(conceptMapExtensionCheck.size() == 0){

          List<StringType> aliases = locationResource.getAlias();
          List<CodeableConcept> types = locationResource.getType();
          if (aliases.size() > 0) {
            for (StringType alias : aliases) {
              String value = alias.toString();

              List<CodeableConcept> existingType = types.stream()
                      .filter(t -> t.getCoding().stream().anyMatch(c -> c.getCode().equals(value)))
                      .collect(Collectors.toList());

              //Check for a value in alias
              if (value != null && existingType.isEmpty()) {
                //Add type to list of existing types
                types.add(FhirHelper.createCodeableConcept(value, "placeholder"));
              }
            }
          }
        }
      }
    }
  }

  @Override
  public void execute(TenantService tenantService, List<DomainResource> data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) throws RuntimeException {
    throw new RuntimeException("Not yet implemented");
  }
}
