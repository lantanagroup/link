package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.model.StoredReportDefinition;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Questionnaire;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/report-definition")
public class ReportDefinitionController extends BaseController {

  /**
   * Responds with a list of the measures stored in the system
   * @param authentication Who the user is authenticated as
   * @param request The REST request
   * @return A list of stored measures
   * @throws Exception
   */
  @GetMapping
  public List<StoredReportDefinition> getMeasures(Authentication authentication, HttpServletRequest request) throws Exception {
    IGenericClient fhirClient = this.getFhirStoreClient(authentication, request);

    // Find all Bundles with the report definition tag
    Bundle searchResults = fhirClient.search()
            .forResource(Bundle.class)
            .withTag(Constants.MainSystem, Constants.ReportDefinitionTag)
            .returnBundle(Bundle.class)
            .execute();

    return searchResults.getEntry().stream().map(e -> {
      Bundle reportDefinitionBundle = (Bundle) e.getResource();
      StoredReportDefinition storedMeasure = new StoredReportDefinition();

      // Determine the name of the report based on the measure or questionnaire within the report definition bundle
      if (reportDefinitionBundle.getEntryFirstRep().getResource() instanceof Measure) {
        Measure measure = (Measure) reportDefinitionBundle.getEntryFirstRep().getResource();
        storedMeasure.setName(measure.hasTitle() ? measure.getTitle() : measure.getName());
      } else if (reportDefinitionBundle.getEntryFirstRep().getResource() instanceof Questionnaire) {
        Questionnaire questionnaire = (Questionnaire) reportDefinitionBundle.getEntryFirstRep().getResource();
        storedMeasure.setName(questionnaire.hasTitle() ? questionnaire.getTitle() : questionnaire.getName());
      }

      if (StringUtils.isEmpty(storedMeasure.getName())) {
        storedMeasure.setName(reportDefinitionBundle.getIdentifier().getValue());
      }

      storedMeasure.setId(reportDefinitionBundle.getIdElement().getIdPart());
      storedMeasure.setSystem(reportDefinitionBundle.getIdentifier().getSystem());
      storedMeasure.setValue(reportDefinitionBundle.getIdentifier().getValue());
      return storedMeasure;
    }).collect(Collectors.toList());
  }
}
