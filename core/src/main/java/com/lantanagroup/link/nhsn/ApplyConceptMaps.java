package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.IReportGenerationEvent;
import com.lantanagroup.link.ResourceIdChanger;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ApplyConceptMaps implements IReportGenerationEvent {

  private static final Logger logger = LoggerFactory.getLogger(ApplyConceptMaps.class);

  private List<ConceptMap> getConceptMaps(ApiConfig config, FhirDataProvider fhirDataProvider) {
    List<ConceptMap> conceptMapsList = new ArrayList();
    if (config.getConceptMaps() != null) {
      // get it from fhirserver
      config.getConceptMaps().stream().forEach(concepMapId -> {
        try {
          IBaseResource conceptMap = fhirDataProvider.getResourceByTypeAndId("ConceptMap", concepMapId);
          conceptMapsList.add((ConceptMap) conceptMap);
        } catch (Exception ex) {
          logger.error(String.format("ConceptMap %s not found", concepMapId));
        }
      });
    }
    return conceptMapsList;
  }

  private void applyMap(ConceptMap map, Coding code) {
    // TODO: Lookup ConceptMap.group based on code system
    map.getGroup().stream().forEach((ConceptMap.ConceptMapGroupComponent group) -> {
      if (group.getSource().equals(code.getSystem())) {
        List<ConceptMap.SourceElementComponent> elements = group.getElement().stream().filter(elem -> elem.getCode().equals(code.getCode())).collect(Collectors.toList());
        // preserve original code
        Extension originalCode = new Extension();
        originalCode.setUrl(Constants.ConceptMappingExtension);
        originalCode.setValue(code.copy());
        code.getExtension().add(originalCode);
        // pick the last element from list
        code.setSystem(group.getTarget());
        code.setDisplay(elements.get(elements.size() - 1).getTarget().get(0).getDisplay());
        code.setCode(elements.get(elements.size() - 1).getTarget().get(0).getCode());
      }
    });
  }

  public void execute(ReportCriteria reportCriteria, ReportContext context, ApiConfig config, FhirDataProvider fhirDataProvider) {
    logger.info("Called: " + ApplyConceptMaps.class.getName());
    List<ConceptMap> conceptMapsList = getConceptMaps(config, fhirDataProvider);
    if (!conceptMapsList.isEmpty()) {
      for (PatientOfInterestModel patientOfInterest : context.getPatientsOfInterest()) {
        logger.info("Patient is: " + patientOfInterest.getId());
        try {
          IBaseResource patientBundle = fhirDataProvider.getBundleById(context.getReportId() + "-" + patientOfInterest.getId().hashCode());
          List<Coding> codes = ResourceIdChanger.findCodings(patientBundle);
          conceptMapsList.stream().forEach(conceptMap -> {
            codes.parallelStream().forEach(code -> {
              this.applyMap(conceptMap, code);
            });
          });
        } catch (Exception ex) {
          logger.error("Exception is: " + ex.getMessage());
        }
      }
    }
  }
}
