package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.*;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class CopyLocationExtensionToType implements IReportGenerationDataEvent {

  private static final Logger logger = LoggerFactory.getLogger(CopyLocationExtensionToType.class);

  @Autowired
  private FhirDataProvider fhirDataProvider;

  @Override
  public void execute(Bundle bundle) {
    //This is a specific transform to move data from an extension to the type of a Location resource for UMich
    //This must happen BEFORE ApplyConceptMaps as an event
    logger.info("Called: " + CopyLocationExtensionToType.class.getName());
    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource().getResourceType().equals(ResourceType.Location)) {
        Location locationResource = (Location) entry.getResource();
        List<Extension> extensions = locationResource.getExtensionsByUrl(Constants.UMichLocationTypeExtensionUrl);
        if (extensions.size() > 0) {
          for (Extension extension : extensions) {
            logger.debug("Moving extension code to type in Location: " + locationResource.getId());
            CodeableConcept extensionValue = (CodeableConcept) extension.getValue();
            List<Coding> extensionValueCodings = extensionValue.getCoding();
            List<CodeableConcept> types = locationResource.getType();
            List<Coding> codings = new ArrayList<>();
            CodeableConcept type = new CodeableConcept();

            for (Coding extensionValueCoding : extensionValueCodings) {
              Coding coding = new Coding();
              coding.setCode(extensionValueCoding.getCode());
              coding.setDisplay(extensionValueCoding.getDisplay());
              coding.setSystem(extensionValueCoding.getSystem());

              //0..* cardinality of coding in Location.type.coding
              codings.add(coding);
            }
            //0..* cardinality of type in Location.type
            type.setCoding(codings);

            //Add type to list of existing types
            types.add(type);
            locationResource.setType(types);
          }
        }
      }
    }
  }

  @Override
  public void execute(List<DomainResource> data) throws RuntimeException{
    throw new RuntimeException("Not yet implemented");
  }
}
