package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.model.StoredReportDefinition;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
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
    FhirDataProvider fhirClient = this.getFhirDataProvider();

    // Find all Bundles with the report definition tag
    Bundle searchResults = fhirClient.searchBundleByTag(Constants.MainSystem, Constants.ReportDefinitionTag);

    return searchResults.getEntry().stream().map(e -> {
      Bundle reportDefinitionBundle = (Bundle) e.getResource();
      StoredReportDefinition storedMeasure = new StoredReportDefinition();

      // Determine the name of the report based on the measure within the report definition bundle
      Optional<Measure> foundMeasure = reportDefinitionBundle.getEntry().stream()
              .filter(e2 -> e2.getResource() instanceof Measure)
              .map(e2 -> (Measure) e2.getResource())
              .findFirst();

      if (foundMeasure.isPresent()) {
        Measure measure = foundMeasure.get();
        storedMeasure.setName(measure.hasTitle() ? measure.getTitle() : measure.getName());
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
