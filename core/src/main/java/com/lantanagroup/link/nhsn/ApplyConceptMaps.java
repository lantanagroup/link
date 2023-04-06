package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.util.BundleUtil;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.IReportGenerationDataEvent;
import com.lantanagroup.link.TenantService;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import lombok.Setter;
import org.hl7.fhir.r4.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ApplyConceptMaps implements IReportGenerationDataEvent {
  private static final Logger logger = LoggerFactory.getLogger(ApplyConceptMaps.class);
  Map<String, ConceptMap> conceptMaps = new HashMap<>();
  DefaultProfileValidationSupport validationSupport;

  @Autowired
  @Setter
  private TenantService tenantService;

  @Autowired
  @Setter
  private ApplyConceptMapsConfig applyConceptMapConfig;

  public ApplyConceptMaps() {
    validationSupport = new DefaultProfileValidationSupport();
    validationSupport.fetchAllStructureDefinitions(FhirContextProvider.getFhirContext());
  }

  private Map<String, ConceptMap> getConceptMaps() {
    if (this.applyConceptMapConfig != null && this.applyConceptMapConfig.getConceptMaps() != null) {
      this.applyConceptMapConfig.getConceptMaps().stream().forEach(cm -> {
        try {
          com.lantanagroup.link.db.model.ConceptMap dbConceptMap = this.tenantService.getConceptMap(cm.getConceptMapId());
          this.conceptMaps.put(cm.getConceptMapId(), dbConceptMap.getResource());
        } catch (Exception ex) {
          logger.error(String.format("ConceptMap %s not found", cm.getConceptMapId()));
        }
      });
    }
    return conceptMaps;
  }

  private FHIRPathEngine getFhirPathEngine() {
    HapiWorkerContext workerContext = new HapiWorkerContext(FhirContextProvider.getFhirContext(), validationSupport);
    return new FHIRPathEngine(workerContext);
  }

  private void translateCoding(ConceptMap map, Coding code) {
    map.getGroup().stream().forEach((ConceptMap.ConceptMapGroupComponent group) -> {
      if (group.getSource().equals(code.getSystem())) {
        List<ConceptMap.SourceElementComponent> elements = group.getElement().stream().filter(elem -> elem.getCode().equals(code.getCode())).collect(Collectors.toList());
        if (elements.size() > 0) {
          // preserve original code
          Extension originalCode = new Extension();
          originalCode.setUrl(Constants.ConceptMappingExtension);
          originalCode.setValue(code.copy());
          code.getExtension().add(originalCode);
          code.setSystem(group.getTarget());
          code.setDisplay(elements.get(elements.size() - 1).getTarget().get(0).getDisplay());
          code.setCode(elements.get(elements.size() - 1).getTarget().get(0).getCode());
        }
      }
    });
  }

  /* TODO
     Move them to a resource filter
   */
  public List<Coding> filterCodingsByPathList(DomainResource resource, List<String> pathList) {
    List<Base> resources = filterResourcesByPathList(resource, pathList);
    List<Coding> codingList = new ArrayList<>();
    for (Base element : resources) {
      if (element instanceof CodeableConcept) {
        codingList.add(((CodeableConcept) element).getCoding().get(0));
      } else if (element instanceof Coding) {
        codingList.add((Coding) element);
      } else if (element instanceof CodeType) {
        CodeableConcept cc = new CodeableConcept();
        cc.addCoding().setCode(((CodeType) element).asStringValue());
        codingList.add(cc.getCoding().get(0));
      }
    }
    return codingList;
  }

  /* TODO
     Move them to a resource filter
   */
  public List<Base> filterResourcesByPathList(DomainResource resource, List<String> pathList) {
    List<Base> results = new ArrayList<>();
    // logger.debug(String.format("FindCodings for resource %s based on path %s", resource.getResourceType() + "/" + resource.getIdElement().getIdPart(), List.of(pathList)));
    pathList.stream().forEach(path -> {
      results.addAll(getFhirPathEngine().evaluate(resource, path));
    });
    return results;
  }

  public List<Coding> applyTransformation(ConceptMap conceptMap, List<Coding> codingList) {
    List<Coding> changedCodes = new ArrayList<>();
    codingList.stream().forEach(coding -> {
      this.translateCoding(conceptMap, coding);
      if (coding.getExtension() != null) {
        changedCodes.add(coding);
      }
    });
    return changedCodes;
  }

  public void execute(Bundle bundle, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    List<DomainResource> resourceList = BundleUtil.toListOfResourcesOfType(FhirContextProvider.getFhirContext(), bundle, DomainResource.class);
    this.execute(resourceList, criteria, context, measureContext);
  }

  public void execute(List<DomainResource> resourceList, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    logger.info("Called: " + ApplyConceptMaps.class.getName());
    if (resourceList.size() > 0) {
      Map<String, ConceptMap> conceptMaps = this.getConceptMaps();
      if (!conceptMaps.isEmpty()) {
        applyConceptMapConfig.getConceptMaps().stream().forEach(conceptMapConfig -> {
          ConceptMap conceptMap = conceptMaps.get(conceptMapConfig.getConceptMapId());
          resourceList.stream().forEach(resource -> {
            List<Coding> codingList = filterCodingsByPathList(resource, conceptMapConfig.getFhirPathContexts());
            if (!codingList.isEmpty()) {
              applyTransformation(conceptMap, codingList);
              // displayTransformation(resource, codingList);
            }
          });
        });
      }
    }
  }
}
