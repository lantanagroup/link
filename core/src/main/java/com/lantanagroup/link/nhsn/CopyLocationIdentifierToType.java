package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.*;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class CopyLocationIdentifierToType implements IReportGenerationDataEvent {

  private static final Logger logger = LoggerFactory.getLogger(CopyLocationIdentifierToType.class);

  @Autowired
  private FhirDataProvider fhirDataProvider;

  @Override
  public void execute(Bundle bundle) {
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
            logger.debug("Moving identifier code to type in Location: " + locationResource.getId());
            List<Coding> codings = new ArrayList<>();
            CodeableConcept type = new CodeableConcept();

            Coding coding = new Coding();
            coding.setCode(identifier.getValue());
            coding.setSystem(identifier.getSystem());

            //0..* cardinality of coding in Location.type.coding
            codings.add(coding);

            //0..* cardinality of type in Location.type
            type.setCoding(codings);

            //Add type to list of existing types
            types.add(type);
          }
          locationResource.setType(types);
        }
      }
    }
  }

  @Override
  public void execute(List<DomainResource> data) throws RuntimeException{
    throw new RuntimeException("Not yet implemented");
  }
}
