package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.model.StoredMeasure;
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
@RequestMapping("/api/measure")
public class MeasureController extends BaseController {

  @GetMapping
  public List<StoredMeasure> getMeasures(Authentication authentication, HttpServletRequest request) throws Exception {
    IGenericClient fhirClient = this.getFhirStoreClient(authentication, request);
    Bundle searchResults = fhirClient.search()
            .forResource(Bundle.class)
            .withTag(Constants.MainSystem, Constants.MeasureBundleTag)
            .returnBundle(Bundle.class)
            .execute();

    return searchResults.getEntry().stream().map(e -> {
      Bundle measureBundle = (Bundle) e.getResource();
      StoredMeasure storedMeasure = new StoredMeasure();

      if (measureBundle.getEntryFirstRep().getResource() instanceof Measure) {
        Measure measure = (Measure) measureBundle.getEntryFirstRep().getResource();
        storedMeasure.setName(measure.hasTitle() ? measure.getTitle() : measure.getName());
      } else if (measureBundle.getEntryFirstRep().getResource() instanceof Questionnaire) {
        Questionnaire questionnaire = (Questionnaire) measureBundle.getEntryFirstRep().getResource();
        storedMeasure.setName(questionnaire.hasTitle() ? questionnaire.getTitle() : questionnaire.getName());
      } else {
        storedMeasure.setName(measureBundle.getIdentifier().getValue());
      }

      storedMeasure.setId(measureBundle.getIdElement().getIdPart());
      storedMeasure.setSystem(measureBundle.getIdentifier().getSystem());
      storedMeasure.setValue(measureBundle.getIdentifier().getValue());
      return storedMeasure;
    }).collect(Collectors.toList());
  }
}
