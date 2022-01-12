package com.lantanagroup.link.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.config.api.ApiConfig;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Collectors;

public class ApiInit {
  private static final Logger logger = LoggerFactory.getLogger(ApiInit.class);


  @Autowired
  private ApiConfig config;

  @Autowired
  private FhirDataProvider provider;

  @Value("classpath:fhir/*")
  private Resource[] resources;

  private void loadReportDefinitions() {

    HttpClient client = HttpClient.newHttpClient();
    FhirContext ctx = FhirContext.forR4();
    IParser jsonParser = ctx.newJsonParser();
    IParser xmlParser = ctx.newXmlParser();

    logger.info("Loading measures defined in configuration...");

    this.config.getReportDefs().getUrls().parallelStream().forEach(reportDefUrl -> {
      logger.info(String.format("Getting the latest report definition from URL %s", reportDefUrl));
      HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create(reportDefUrl))
              .build();

      Integer retryCount = 0;
      String content = null;

      while (Strings.isEmpty(content) && retryCount <= this.config.getReportDefs().getMaxRetry()) {
        try {
          HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
          content = response.body();
        } catch (ConnectException ex) {
          retryCount++;

          logger.error(String.format("Error loading report definition from URL %s due to a connection issue", reportDefUrl));
          if (retryCount <= this.config.getReportDefs().getMaxRetry()) {
            logger.info(String.format("Retrying to retrieve report definition in %s seconds...", this.config.getReportDefs().getRetryWait() / 1000));
          } else if (this.config.getReportDefs().getRetryWait() <= 0) {
            logger.error("System not configured with api.report-defs.retry-wait. Won't retry.");
            return;
          } else {
            logger.error(String.format("Reached maximum retry attempts for report definition %s", reportDefUrl));
            return;
          }

          try {
            Thread.sleep(this.config.getReportDefs().getRetryWait());
          } catch (InterruptedException ie) {
            return;
          }
        } catch (Exception ex) {
          logger.error(String.format("Error loading report def from URL %s due to %s", reportDefUrl, ex.getMessage()));
          return;
        }
      }

      if (Strings.isEmpty(content)) {
        logger.error(String.format("Could not retrieve report definition at %s", reportDefUrl));
        return;
      }
      Bundle reportDefBundle = FhirHelper.getBundle(content);
      if (reportDefBundle == null) {
        logger.error(String.format("Error parsing report def bundle from %s", reportDefUrl));
        return;
      }

      // Make sure the report def bundle has an identifier
      if (reportDefBundle.getIdentifier() == null) {
        logger.error(String.format("Report definition from URL %s does not have an identifier to distinguish itself", reportDefUrl));
        return;
      }

      logger.info(String.format("Retrieved and parsed %s (%s) report def. Storing the report def in internal store.", reportDefBundle.getIdentifier().getValue(), reportDefBundle.getIdentifier().getSystem()));

      Bundle searchResults = null;
      retryCount = 0;

      while (searchResults == null && retryCount <= this.config.getReportDefs().getMaxRetry()) {
        try {
          // Search to see if the report def bundle already exists

          searchResults = provider.searchReportDefinition(reportDefBundle.getIdentifier().getSystem(), reportDefBundle.getIdentifier().getValue());
        } catch (FhirClientConnectionException fcce) {
          retryCount++;

          logger.error(String.format("Error storing report definition from URL %s in internal FHIR store due to a connection issue", reportDefUrl));
          if (retryCount <= this.config.getReportDefs().getMaxRetry()) {
            logger.info(String.format("Retrying to store report definition in %s seconds...", this.config.getReportDefs().getRetryWait() / 1000));
          } else if (this.config.getReportDefs().getRetryWait() <= 0) {
            logger.error("System not configured with api.report-defs.retry-wait. Won't retry.");
            return;
          } else {
            logger.error(String.format("Reached maximum retry attempts to store report definition %s", reportDefUrl));
            return;
          }

          try {
            Thread.sleep(this.config.getReportDefs().getRetryWait());
          } catch (InterruptedException ie) {
            return;
          }
        } catch (Exception ex) {
          logger.error(String.format("Error storing report def from URL %s due to %s", reportDefUrl, ex.getMessage()));
          return;
        }
      }

      // If none found, create. If one found, update. If more than one found, respond with error.
      if (searchResults.getEntry().size() == 0) {
        reportDefBundle.setId((String) null);
        reportDefBundle.setMeta(new Meta());
        reportDefBundle.getMeta().addTag(Constants.MainSystem, Constants.ReportDefinitionTag, null);
        provider.createResource(reportDefBundle);
        logger.info(String.format("Created report def bundle from URL %s as ID %s", reportDefUrl, reportDefBundle.getIdElement().getIdPart()));
      } else if (searchResults.getEntry().size() == 1) {
        Bundle foundReportDefBundle = (Bundle) searchResults.getEntryFirstRep().getResource();
        reportDefBundle.setId(foundReportDefBundle.getIdElement().getIdPart());
        reportDefBundle.setMeta(foundReportDefBundle.getMeta());
        provider.updateResource(reportDefBundle);
        logger.info(String.format("Updated report def bundle from URL %s with ID %s", reportDefUrl, reportDefBundle.getIdElement().getIdPart()));
      } else {
        logger.error(String.format("Found multiple report def bundles with identifier %s|%s", reportDefBundle.getIdentifier().getSystem(), reportDefBundle.getIdentifier().getValue()));
        return;
      }
    });
  }

  private void loadSearchParameters() {
    try {
      FhirContext ctx = FhirContext.forR4();
      IParser xmlParser = ctx.newXmlParser();
      for (final Resource res : resources) {
        IBaseResource resource = readFileAsFhirResource(xmlParser, res.getInputStream());
        provider.updateResource(resource);
      }
    } catch (Exception ex) {
      logger.error(String.format("Error in loadSearchParameters due to %s", ex.getMessage()));
    }
  }

  private IBaseResource readFileAsFhirResource(IParser xmlParser, InputStream file) {
    String resourceString = new BufferedReader(new InputStreamReader(file)).lines().parallel().collect(Collectors.joining("\n"));
    return xmlParser.parseResource(resourceString);
  }

  public void init() {
    this.loadReportDefinitions();
    this.loadSearchParameters();
  }
}
