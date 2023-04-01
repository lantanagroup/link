package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.api.EncodingEnum;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.MongoService;
import com.lantanagroup.link.db.model.MeasureDefinition;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/measureDef")
public class MeasureDefController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(MeasureDefController.class);

  @Autowired
  private MongoService mongoService;

  @Autowired
  private ApiConfig apiConfig;

  /**
   * Executes the measure bundle on the evaluation service (cqf-ruler)
   */
  private void executeBundle(Bundle bundle) {
    FhirDataProvider fhirDataProvider = new FhirDataProvider(this.apiConfig.getEvaluationService());

    bundle.setType(Bundle.BundleType.BATCH);
    bundle.getEntry().forEach(entry -> {
      entry.setRequest(new Bundle.BundleEntryRequestComponent());
      entry.getRequest()
              .setMethod(Bundle.HTTPVerb.PUT)
              .setUrl(entry.getResource().getResourceType().toString() + "/" + entry.getResource().getIdElement().getIdPart());
    });

    logger.info("Loading measure definition {} on eval service {}", bundle.getIdElement().getIdPart(), this.apiConfig.getEvaluationService());
    fhirDataProvider.transaction(bundle);
  }

  private Bundle getBundleFromUrl(String url) throws URISyntaxException {
    URI uri = new URI(url);

    if (this.config.isRequireHttps() && !"https".equalsIgnoreCase(uri.getScheme())) {
      throw new IllegalStateException("URL is not HTTPS");
    }

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri);
    HttpRequest request = requestBuilder.build();
    HttpResponse<String> response = null;

    logger.info("Loading measure definition from URL {}", url);

    try {
      response = client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      logger.warn("Error retrieving measure def", e);
      return null;
    } catch (InterruptedException e) {
      logger.warn("Error retrieving measure def", e);
      return null;
    }

    String responseBody = response.body();

    logger.debug("Retrieved measure definition, parsing...");

    // Parse and validate remote bundle
    return EncodingEnum.detectEncoding(responseBody)
            .newParser(FhirContextProvider.getFhirContext())
            .parseResource(Bundle.class, responseBody);
  }

  @PutMapping
  public void createOrUpdateMeasureDef(@RequestBody(required = false) Bundle bundleBody, @RequestParam(required = false) String url) throws Exception {
    if (bundleBody == null && url == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either a Bundle must be specified in a JSON body of the request, or a \"url\" query parameter must be specified");
    }

    Bundle bundle = bundleBody == null ? this.getBundleFromUrl(url) : bundleBody;

    if (bundle == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body must specify a FHIR bundle");
    }

    if (!bundle.hasId()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bundle must specify an id");
    }

    if (!FhirHelper.validLibraries(bundle)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Measure bundle contains libraries without data requirements");
    }

    if (bundle.getEntry().stream().anyMatch(entry -> !entry.getResource().hasId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Measure bundle contains resources without ids");
    }

    this.executeBundle(bundle);

    MeasureDefinition measureDefinition = this.mongoService.findMeasureDefinition(bundle.getIdElement().getIdPart());

    if (measureDefinition == null) {
      measureDefinition = new MeasureDefinition();
      measureDefinition.setMeasureId(bundle.getIdElement().getIdPart());
    }

    measureDefinition.setLastUpdated(new Date());
    measureDefinition.setBundle(bundle);

    logger.info("Persisting measure definition {} in database", bundle.getIdElement().getIdPart());
    this.mongoService.saveMeasureDefinition(measureDefinition);
  }

  @GetMapping
  public List<MeasureDefinition> searchMeasureDefinitions() {
    return this.mongoService.getAllMeasureDefinitions();
  }
}
