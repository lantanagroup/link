package com.lantanagroup.link.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.config.api.ApiConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiInit {
  private static final Logger logger = LoggerFactory.getLogger(ApiInit.class);

  @Autowired
  private ApiConfig config;

  private void loadMeasures() {
    HttpClient client = HttpClient.newHttpClient();
    FhirContext ctx = FhirContext.forR4();
    IParser jsonParser = ctx.newJsonParser();
    IParser xmlParser = ctx.newXmlParser();

    logger.info("Loading measures defined in configuration...");

    this.config.getMeasures().forEach(measureConfig -> {
      logger.info("Getting the latest measure definition for " + measureConfig.getId() + " from URL " + measureConfig.getUrl());
      HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create(measureConfig.getUrl()))
              .build();

      String content = null;
      try {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        content = response.body();
      } catch (ConnectException ex) {
        logger.error(String.format("Error loading measure from URL %s due to a connection issue", measureConfig.getUrl()));
      } catch (Exception ex) {
        logger.error(String.format("Error loading measure from URL %s due to %s", measureConfig.getUrl(), ex.getMessage()));
        return;
      }

      Bundle measureBundle = null;
      try {
        if (content.trim().startsWith("{") || content.trim().startsWith("[")) {
          measureBundle = jsonParser.parseResource(Bundle.class, content);
        } else {
          measureBundle = xmlParser.parseResource(Bundle.class, content);
        }
      } catch (Exception ex) {
        logger.error(String.format("Error parsing measure bundle from URL %s due to %s", measureConfig.getUrl(), ex.getMessage()));
        return;
      }

      // Make sure the measure bundle has an identifier
      if (measureBundle.getIdentifier() == null) {
        logger.error(String.format("Measure bundle from URL %s does not have an identifier to distinguish itself", measureConfig.getUrl()));
        return;
      }

      // Search to see if the measure bundle already exists
      IGenericClient fhirClient = ctx.newRestfulGenericClient(this.config.getFhirServerStore());
      Bundle searchResults = fhirClient.search()
              .forResource("Bundle")
              .withTag(Constants.MainSystem, Constants.ReportDefinitionTag)
              .where(Bundle.IDENTIFIER.exactly().systemAndCode(measureBundle.getIdentifier().getSystem(), measureBundle.getIdentifier().getValue()))
              .returnBundle(Bundle.class)
              .execute();

      // If none found, create. If one found, update. If more than one found, respond with error.
      if (searchResults.getEntry().size() == 0) {
        measureBundle.setId((String)null);
        measureBundle.setMeta(new Meta());
        measureBundle.getMeta().addTag(Constants.MainSystem, Constants.ReportDefinitionTag, null);
        fhirClient.create().resource(measureBundle).execute();
        logger.info(String.format("Created measure bundle from URL %s as ID %s", measureConfig.getUrl(), measureBundle.getIdElement().getIdPart()));
      } else if (searchResults.getEntry().size() == 1) {
        Bundle foundMeasureBundle = (Bundle) searchResults.getEntryFirstRep().getResource();
        measureBundle.setId(foundMeasureBundle.getIdElement().getIdPart());
        measureBundle.setMeta(foundMeasureBundle.getMeta());
        fhirClient.update().resource(measureBundle).execute();
        logger.info(String.format("Updated measure bundle from URL %s with ID %s", measureConfig.getUrl(), measureBundle.getIdElement().getIdPart()));
      } else {
        logger.error(String.format("Found multiple measure bundles with identifier %s|%s", measureBundle.getIdentifier().getSystem(), measureBundle.getIdentifier().getValue()));
        return;
      }
    });
  }

  public void init() {
    this.loadMeasures();
  }
}
