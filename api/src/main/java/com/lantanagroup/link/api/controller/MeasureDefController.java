package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.api.EncodingEnum;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.api.MeasureServiceWrapper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.model.MeasureDefinition;
import com.lantanagroup.link.db.model.MeasurePackage;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.WebDataBinder;
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
  private SharedService sharedService;

  @Autowired
  private ApiConfig apiConfig;

  @InitBinder
  public void initializeBinder(WebDataBinder binder) {
    binder.setDisallowedFields();
  }

  private Bundle getBundleFromUrl(String url) throws URISyntaxException {
    URI uri = new URI(Helper.sanitizeUrl(url));

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
      Thread.currentThread().interrupt();
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
  public void createOrUpdateMeasureDef(@RequestBody(required = false) Bundle bundleBody, @RequestParam(required = false) String measureId) throws Exception {
    if (bundleBody == null && StringUtils.isEmpty(measureId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either a Bundle must be specified in a JSON body of the request, or a \"measureId\" query parameter must be specified");
    }

    if (StringUtils.isNotEmpty(measureId) && this.apiConfig.getMeasureDefUrls().get(measureId) == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The specified measureId is not configured with a measure definition URL");
    }

    String url = StringUtils.isNotEmpty(measureId) ? this.apiConfig.getMeasureDefUrls().get(measureId) : null;
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

    MeasureDefinition measureDefinition = this.sharedService.getMeasureDefinition(bundle.getIdElement().getIdPart());

    if (measureDefinition == null) {
      measureDefinition = new MeasureDefinition();
      measureDefinition.setMeasureId(bundle.getIdElement().getIdPart());
    }

    measureDefinition.setLastUpdated(new Date());
    measureDefinition.setBundle(bundle);

    logger.info("Persisting measure definition {} in database", bundle.getIdElement().getIdPart());
    this.sharedService.saveMeasureDefinition(measureDefinition);
  }

  @GetMapping
  public List<MeasureDefinition> searchMeasureDefinitions() {
    return this.sharedService.getMeasureDefinitions();
  }

  @DeleteMapping("/{measureId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteMeasureDefinition(@PathVariable String measureId) {
    MeasureDefinition measureDefinition = this.sharedService.getMeasureDefinition(measureId);

    if (measureDefinition == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Could not find measure definition %s", measureId));
    }

    this.sharedService.deleteMeasureDefinition(measureId);

    // Make sure the any packages that reference the measure definition are updated to remove the reference to the definition
    List<MeasurePackage> measurePackages = this.sharedService.getMeasurePackages();
    measurePackages.forEach(measurePackage -> {
      if (measurePackage.getMeasureIds().remove(measureId)) {
        this.sharedService.saveMeasurePackage(measurePackage);
      }
    });
  }

  @PostMapping("/{measureId}/$evaluate")
  public MeasureReport evaluate(@PathVariable String measureId, @RequestBody Parameters parameters) {
    MeasureDefinition measureDefinition = this.sharedService.getMeasureDefinition(measureId);

    if (measureDefinition == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Could not find measure definition %s", measureId));
    }

    MeasureServiceWrapper measureServiceWrapper = new MeasureServiceWrapper(measureDefinition.getBundle(), null);

    DateTimeType periodStart;
    DateTimeType periodEnd;
    StringType subject;
    Bundle additionalData;
    try {
      periodStart = (DateTimeType) parameters.getParameterValue("periodStart");
      periodEnd = (DateTimeType) parameters.getParameterValue("periodEnd");
      subject = (StringType) parameters.getParameterValue("subject");
      additionalData = (Bundle) parameters.getParameter("additionalData").getResource();
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse request body", e);
    }

    return measureServiceWrapper.evaluate(
            periodStart.asStringValue(),
            periodEnd.asStringValue(),
            subject.asStringValue(),
            additionalData);
  }
}
