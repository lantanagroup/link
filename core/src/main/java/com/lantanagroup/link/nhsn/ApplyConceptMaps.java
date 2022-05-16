package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.IReportGenerationEvent;
import com.lantanagroup.link.ResourceIdChanger;
import com.lantanagroup.link.model.QueryResponse;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ConceptMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class ApplyConceptMaps implements IReportGenerationEvent {

  private static final Logger logger = LoggerFactory.getLogger(ApplyConceptMaps.class);


  private void getConvertCode(ConceptMap map, Coding code) {
    // TODO: Lookup ConceptMap.group based on code system
    map.getGroup().stream().forEach((ConceptMap.ConceptMapGroupComponent group) -> {
      if (group.getSource().equals(code.getSystem())) {
        List<ConceptMap.SourceElementComponent> elements = group.getElement().stream().filter(elem -> elem.getCode().equals(code.getCode())).collect(Collectors.toList());
        // pick the last element from list
        code.setSystem(group.getTarget());
        code.setDisplay(elements.get(elements.size() - 1).getTarget().get(0).getDisplay());
        code.setCode(elements.get(elements.size() - 1).getTarget().get(0).getCode());
      }
    });
  }


  public void execute(ReportCriteria reportCriteria, ReportContext context) {
    logger.info("Called: " + ApplyConceptMaps.class.getName());
    List<QueryResponse> patientQueryResponses = context.getPatientData();
    for (QueryResponse patientQueryResponse : patientQueryResponses) {
      List<ConceptMap> conceptMapsList = context.getConceptMaps();

      if (!conceptMapsList.isEmpty()) {
        List<Coding> codes = ResourceIdChanger.findCodings(patientQueryResponse);
        conceptMapsList.stream().forEach(conceptMap -> {
          codes.stream().forEach(code -> {
            this.getConvertCode(conceptMap, code);
          });
        });
      }
    }

  }
}
