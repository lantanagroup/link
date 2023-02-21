package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.StoredMeasure;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/measure")
public class ReportDefinitionController extends BaseController {

  @Setter
  @Autowired
  ApiConfig config;

  /**
   * Responds with a list of the measures stored in the system
   *
   * @param authentication Who the user is authenticated as
   * @param request        The REST request
   * @return A list of stored measures
   * @throws Exception
   */
  @GetMapping
  public List<StoredMeasure> getMeasures(Authentication authentication, HttpServletRequest request) throws Exception {
    FhirDataProvider fhirClient = this.getFhirDataProvider();

    // Find all Bundles with the measure tag
    Bundle searchResults = fhirClient.searchBundleByTag(Constants.MainSystem, Constants.ReportDefinitionTag);

    List<StoredMeasure> storedMeasures = searchResults.getEntry().stream().map(e -> {
      Bundle reportDefinitionBundle = (Bundle) e.getResource();
      StoredMeasure storedMeasure = new StoredMeasure();

      // Determine the name of the report based on the measure within the measure bundle
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

      storedMeasure.getBundleIds().add(reportDefinitionBundle.getIdElement().getIdPart());
      return storedMeasure;
    }).collect(Collectors.toList());
    //  add the multi-measure reports
    if(config.getMeasurePackages() != null && !config.getMeasurePackages().isEmpty()) {
      config.getMeasurePackages().forEach(apiMeasurePackage -> {
        String[] bundleIds = apiMeasurePackage.getBundleIds();
        List<StoredMeasure> multiStoredMeasures = storedMeasures.stream().filter(storedMeasure -> List.of(bundleIds).contains(storedMeasure.getId())).collect(Collectors.toList());
        StoredMeasure multiMeasure = new StoredMeasure();
        multiMeasure.setName(multiStoredMeasures.stream().map(storedMeasure -> storedMeasure.getName()).collect(Collectors.joining("-")));
        multiMeasure.setId(apiMeasurePackage.getId());
        for (StoredMeasure multiStoreMeasure : multiStoredMeasures) {
          multiMeasure.getBundleIds().add(multiStoreMeasure.getId());
        }
        storedMeasures.add(multiMeasure);
      });
    }
    return storedMeasures;
  }
}
