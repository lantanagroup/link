package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.IReportGenerationEvent;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
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
public class ApplyConceptMaps implements IReportGenerationEvent {

  private static final Logger logger = LoggerFactory.getLogger(ApplyConceptMaps.class);
  Map<String, ConceptMap> conceptMapMap = new HashMap<>();

  @Autowired
  @Setter
  private ApplyConceptMapsConfig applyConceptMapConfig;

  @Autowired
  @Setter
  private FhirDataProvider fhirDataProvider;


  private Map<String, ConceptMap> getConceptMaps(FhirDataProvider fhirDataProvider) {
    if (applyConceptMapConfig != null && applyConceptMapConfig.getConceptMaps() != null) {
      // get it from fhirserver
      applyConceptMapConfig.getConceptMaps().stream().forEach(concepMap -> {
        try {
          IBaseResource conceptMap = fhirDataProvider.getResourceByTypeAndId("ConceptMap", concepMap.getConceptMapId());
          conceptMapMap.put(concepMap.getConceptMapId(), (ConceptMap) conceptMap);
        } catch (Exception ex) {
          logger.error(String.format("ConceptMap %s not found", concepMap.getConceptMapId()));
        }
      });
    }
    return conceptMapMap;
  }

  public void applyMap(ConceptMap map, Coding code) {
    map.getGroup().stream().forEach((ConceptMap.ConceptMapGroupComponent group) -> {
      if (group.getSource().equals(code.getSystem())) {
        List<ConceptMap.SourceElementComponent> elements = group.getElement().stream().filter(elem -> elem.getCode().equals(code.getCode())).collect(Collectors.toList());
        // preserve original code
        Extension originalCode = new Extension();
        originalCode.setUrl(Constants.ConceptMappingExtension);
        originalCode.setValue(code.copy());
        code.getExtension().add(originalCode);
        code.setSystem(group.getTarget());
        code.setDisplay(elements.get(elements.size() - 1).getTarget().get(0).getDisplay());
        code.setCode(elements.get(elements.size() - 1).getTarget().get(0).getCode());
      }
    });
  }

  private void addCode(ArrayList codes, List<Base> results, int i) {
    if (results.get(i) instanceof CodeableConcept) {
      CodeableConcept cc = (CodeableConcept) results.get(i);
      codes.add(cc.getCoding().get(0));
    } else if (results.get(i) instanceof Coding) {
      Coding cc = (Coding) results.get(i);
      codes.add(cc);
    } else if (results.get(i) instanceof CodeType) {
      CodeType c = (CodeType) results.get(i);
      CodeableConcept cc = new CodeableConcept();
      cc.addCoding().setCode(c.asStringValue());
      codes.add(cc.getCoding().get(0));
    }
  }

  public List<Coding> findCodings(List<String> pathList, Bundle patientBundle) {
    HapiWorkerContext workerContext = new HapiWorkerContext(FhirContextProvider.getFhirContext(), new DefaultProfileValidationSupport());
    FHIRPathEngine fhirPathEngine = new FHIRPathEngine(workerContext);

    ArrayList codes = new ArrayList();
    for (Bundle.BundleEntryComponent entry : patientBundle.getEntry()) {
      Resource resource = entry.getResource();
      for (int j = 0; j < pathList.size(); j++) {
        List<Base> results = fhirPathEngine.evaluate(resource, pathList.get(j));
        if (results.isEmpty()) continue;
        for (int i = 0; i < results.size(); i++) {
          addCode(codes, results, i);
        }
      }
    }
    return codes;
  }

  public void execute(ReportCriteria reportCriteria, ReportContext context) {
    logger.info("Called: " + ApplyConceptMaps.class.getName());

    Map<String, ConceptMap> conceptMapsMap = getConceptMaps(fhirDataProvider);
    if (!conceptMapsMap.isEmpty()) {
      for (PatientOfInterestModel patientOfInterest : context.getPatientsOfInterest()) {
        // logger.info("Patient is: " + patientOfInterest.getId());
        if(!StringUtils.isEmpty(patientOfInterest.getId())){
          IBaseResource patientBundle = fhirDataProvider.getBundleById(context.getReportId() + "-" + patientOfInterest.getId().hashCode());
          applyConceptMapConfig.getConceptMaps().stream().forEach(conceptMap -> {
            List<Coding> codes = this.findCodings(conceptMap.getFhirPathContexts(), (Bundle) patientBundle);
            codes.stream().forEach(code -> {
              this.applyMap(conceptMapsMap.get(conceptMap.getConceptMapId()), code);
            });
          });
          fhirDataProvider.updateResource(patientBundle);
        }
      }
    }
  }
}

