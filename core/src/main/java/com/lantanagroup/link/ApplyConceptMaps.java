package com.lantanagroup.link;

import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.util.BundleUtil;
import com.lantanagroup.link.db.TenantService;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ApplyConceptMaps {
  private static final Logger logger = LoggerFactory.getLogger(ApplyConceptMaps.class);
  private final DefaultProfileValidationSupport validationSupport = new DefaultProfileValidationSupport(FhirContextProvider.getFhirContext());

  private List<com.lantanagroup.link.db.model.ConceptMap> conceptMaps;

  public static boolean isMapped(Coding coding, String system, String code) {
    return coding.getExtensionsByUrl(Constants.ConceptMappingExtension).stream()
            .map(Extension::getValue)
            .filter(value -> value instanceof Coding)
            .map(value -> (Coding) value)
            .anyMatch(mappedCoding -> mappedCoding.is(system, code));
  }

  public ApplyConceptMaps() {
    validationSupport.fetchAllStructureDefinitions();
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
      results.addAll(FhirHelper.getFhirPathEngine().evaluate(resource, path));
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

  public void execute(TenantService tenantService, Bundle bundle) {
    List<DomainResource> resourceList = BundleUtil.toListOfResourcesOfType(FhirContextProvider.getFhirContext(), bundle, DomainResource.class);
    this.execute(tenantService, resourceList);
  }

  public void execute(TenantService tenantService, List<DomainResource> resourceList) {
    if (resourceList.size() > 0) {
      if (this.conceptMaps == null) {
        logger.info("Getting all concept maps for the tenant {}", tenantService.getConfig().getId());
        this.conceptMaps = tenantService.getAllConceptMaps();
        logger.info("Found {} concept maps for tenant {}", this.conceptMaps.size(), tenantService.getConfig().getId());
      }

      for (com.lantanagroup.link.db.model.ConceptMap dbConceptMap : this.conceptMaps) {
        ConceptMap conceptMap = dbConceptMap.getConceptMap();

        logger.debug("Applying concept map {} to {} resources", dbConceptMap.getId(), resourceList.size());

        resourceList.stream().forEach(resource -> {
          List<Coding> codingList = filterCodingsByPathList(resource, dbConceptMap.getContexts());
          if (!codingList.isEmpty()) {
            this.applyTransformation(conceptMap, codingList);
          }
        });
      }
    }
  }
}
