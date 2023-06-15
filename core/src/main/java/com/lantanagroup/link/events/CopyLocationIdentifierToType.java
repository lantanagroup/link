package com.lantanagroup.link.events;

import com.lantanagroup.link.IReportGenerationDataEvent;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
        List<Identifier> identifiers = locationResource.getIdentifier();
        List<CodeableConcept> types = locationResource.getType();
        if (identifiers.size() > 0) {
          for (Identifier identifier : identifiers) {
            String idValue = identifier.getValue();
            String idSystem = identifier.getSystem();

            //Check for a full identifier (both system and value)
            if(idValue != null && idSystem != null){
              //Check for if the identifier of the Location has already been copied over into Location.type
              Boolean found = false;
              if(types.size() > 0){
                for(CodeableConcept type : types){
                  for(Coding coding : type.getCoding()){
                    if(coding.getCode().equals(idValue) && coding.getSystem().equals(idSystem)){
                      found = true;
                      break;
                    }
                  }
                  if(found) break;
                }
              }
              //If no duplicate, then copy/paste into Location.type
              if(!found){
                // logger.debug("Moving identifier code to type in Location: " + locationResource.getId());
                List<Coding> codings = new ArrayList<>();
                CodeableConcept type = new CodeableConcept();

                Coding coding = new Coding();

                coding.setCode(idValue);
                coding.setSystem(idSystem);

                //0..* cardinality of coding in Location.type.coding
                codings.add(coding);

                //0..* cardinality of type in Location.type
                type.setCoding(codings);

                //Add type to list of existing types
                types.add(type);
              }
            }
          }
          if(types.size() > 0) locationResource.setType(types);
        }
      }
    }
  }

  @Override
  public void execute(TenantService tenantService, List<DomainResource> data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) throws RuntimeException {
    throw new RuntimeException("Not yet implemented");
  }
}
