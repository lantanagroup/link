package com.lantanagroup.link.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.auth.LinkOAuthConfig;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Measure;
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
import java.util.Optional;
import java.util.stream.Collectors;

public class ApiInit {
  private static final Logger logger = LoggerFactory.getLogger(ApiInit.class);
  @Setter
  protected FhirContext ctx = FhirContextProvider.getFhirContext();
  @Autowired
  private ApiConfig config;
  @Autowired
  private QueryConfig queryConfig;

  @Autowired
  private USCoreConfig usCoreConfig;

  @Autowired
  private FhirDataProvider provider;

  @Value("classpath:fhir/*")
  private Resource[] resources;

  private void loadMeasureDefinitions() {
    HttpClient client = HttpClient.newHttpClient();

    logger.info("Loading measures defined in configuration...");

    if (this.queryConfig.isRequireHttps() && !this.queryConfig.getFhirServerBase().toLowerCase().startsWith("https://")) {
      logger.error("Error, Query URL requires https");
      return;
    }

    this.config.getReportDefs().getUrls().parallelStream().forEach(measureDef -> {
      String measureDefUrl = measureDef.getUrl();
      logger.info(String.format("Getting the latest measure from URL %s", measureDefUrl));

      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
              .uri(URI.create(measureDefUrl));

      //check if report-defs config has auth properties, if so generate token and add to request
      LinkOAuthConfig authConfig = config.getReportDefs().getAuth();
      if (authConfig != null) {
        try {
          String token = OAuth2Helper.getToken(authConfig);
          requestBuilder.setHeader("Authorization", "Bearer " + token);
        } catch (Exception ex) {
          logger.error(String.format("Error generating authorization token: %s", ex.getMessage()));
          return;
        }
      }

      HttpRequest request = requestBuilder.build();

//      HttpRequest request = HttpRequest.newBuilder()
//              .uri(URI.create(measureDefUrl))
//              .build();

      if (this.config.isRequireHttps() && !measureDefUrl.contains("https")) {
        logger.error(String.format("https required for measure definition url: ", measureDefUrl));
        return;
      }

      Integer retryCount = 0;
      String content = null;

      while (Strings.isEmpty(content) && retryCount <= this.config.getReportDefs().getMaxRetry()) {
        try {
          HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
          content = response.body();
        } catch (ConnectException ex) {
          retryCount++;

          logger.error(String.format("Error loading measure from URL %s due to a connection issue", measureDefUrl));
          if (retryCount <= this.config.getReportDefs().getMaxRetry()) {
            logger.info(String.format("Retrying to retrieve measure in %s seconds...", this.config.getReportDefs().getRetryWait() / 1000));
          } else if (this.config.getReportDefs().getRetryWait() <= 0) {
            logger.error("System not configured with api.report-defs.retry-wait. Won't retry.");
            return;
          } else {
            logger.error(String.format("Reached maximum retry attempts for measure %s", measureDefUrl));
            return;
          }

          try {
            Thread.sleep(this.config.getReportDefs().getRetryWait());
          } catch (InterruptedException ie) {
            return;
          }
        } catch (Exception ex) {
          logger.error(String.format("Error loading report def from URL %s due to %s", measureDefUrl, ex.getMessage()));
          return;
        }
      }

      if (Strings.isEmpty(content)) {
        logger.error(String.format("Could not retrieve measure at %s", measureDefUrl));
        return;
      }

      Bundle measureDefBundle = FhirHelper.getBundle(content);

      if (measureDefBundle == null) {
        logger.error(String.format("Error parsing report def bundle from %s", measureDefUrl));
        return;
      }

      measureDefBundle = FhirHelper.storeTerminologyAndReturnOther(measureDefBundle, this.config);

      Optional<Measure> foundMeasure = measureDefBundle.getEntry().stream()
              .filter(e -> e.getResource() instanceof Measure)
              .map(e -> (Measure) e.getResource())
              .findFirst();

      if (!foundMeasure.isPresent()) {
        logger.error(String.format("Measure definition bundle from %s does not include a Measure resource", measureDefUrl));
        return;
      }

      if (!FhirHelper.validLibraries(measureDefBundle)) {
        logger.error(String.format("Measure definition bundle from %s contains libraries without dataRequirements.", measureDefUrl));
        return;
      }

      String missingResourceTypes = FhirHelper.getQueryConfigurationDataReqMissingResourceTypes(FhirHelper.getQueryConfigurationResourceTypes(usCoreConfig), measureDefBundle);
      if (!missingResourceTypes.equals("")) {
        logger.error(String.format("These resource types %s are in data requirements for %s but missing from the configuration.", missingResourceTypes, measureDefUrl));
        // return;
      }

      Measure measure = foundMeasure.get();

      Identifier defaultIdentifier = new Identifier()
              .setSystem(Constants.MainSystem)
              .setValue(measureDefBundle.getIdElement().getIdPart());

      if (measure.getIdentifier().size() == 0) {
        measure.addIdentifier(defaultIdentifier);
      } else {
        defaultIdentifier = measure.getIdentifierFirstRep();
      }

      // Make sure the report def bundle has an identifier
      if (measureDefBundle.getIdentifier().isEmpty()) {
        measureDefBundle.setIdentifier(defaultIdentifier);
      }

      logger.info(String.format("Retrieved and parsed %s (%s) report def. Storing the report def in internal store.", measureDefBundle.getIdentifier().getValue(), measureDefBundle.getIdentifier().getSystem()));

      Bundle searchResults = null;
      retryCount = 0;

      while (searchResults == null && retryCount <= this.config.getReportDefs().getMaxRetry()) {
        try {
          // Search to see if the report def bundle already exists

          searchResults = provider.searchReportDefinition(measureDefBundle.getIdentifier().getSystem(), measureDefBundle.getIdentifier().getValue());
        } catch (FhirClientConnectionException fcce) {
          retryCount++;

          logger.error(String.format("Error storing measure from URL %s in internal FHIR store due to a connection issue", measureDefUrl));
          if (retryCount <= this.config.getReportDefs().getMaxRetry()) {
            logger.info(String.format("Retrying to store measure in %s seconds...", this.config.getReportDefs().getRetryWait() / 1000));
          } else if (this.config.getReportDefs().getRetryWait() <= 0) {
            logger.error("System not configured with api.report-defs.retry-wait. Won't retry.");
            return;
          } else {
            logger.error(String.format("Reached maximum retry attempts to store measure %s", measureDefUrl));
            return;
          }

          try {
            Thread.sleep(this.config.getReportDefs().getRetryWait());
          } catch (InterruptedException ie) {
            return;
          }
        } catch (Exception ex) {
          logger.error(String.format("Error storing report def from URL %s due to %s", measureDefUrl, ex.getMessage()));
          return;
        }
      }

      // If none found, create. If one found, update. If more than one found, respond with error.
      if (searchResults.getEntry().size() == 0) {
        measureDefBundle.setId((String) null);
        measureDefBundle.setMeta(new Meta());
        measureDefBundle.getMeta().addTag(Constants.MainSystem, Constants.ReportDefinitionTag, null);
        provider.createResource(measureDefBundle);
        logger.info(String.format("Created report def bundle from URL %s as ID %s", measureDefUrl, measureDefBundle.getIdElement().getIdPart()));
      } else if (searchResults.getEntry().size() == 1) {
        Bundle foundReportDefBundle = (Bundle) searchResults.getEntryFirstRep().getResource();
        measureDefBundle.setId(foundReportDefBundle.getIdElement().getIdPart());
        measureDefBundle.setMeta(foundReportDefBundle.getMeta());
        provider.updateResource(measureDefBundle);
        logger.info(String.format("Updated report def bundle from URL %s with ID %s", measureDefUrl, measureDefBundle.getIdElement().getIdPart()));
      } else {
        logger.error(String.format("Found multiple report def bundles with identifier %s|%s", measureDefBundle.getIdentifier().getSystem(), measureDefBundle.getIdentifier().getValue()));
        return;
      }
    });
  }

  private void loadSearchParameters() {
    try {
      FhirContext ctx = FhirContextProvider.getFhirContext();
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

  private int getSocketTimout() {
    int socketTimeout = 30 * 1000; // 30 sec // 200 * 5000
    if (config.getSocketTimeout() != null) {
      try {
        socketTimeout = Integer.parseInt(config.getSocketTimeout());
      } catch (Exception ex) {
        logger.error(String.format("Error % in socket-timeout %s ", ex.getMessage(), config.getSocketTimeout()));
      }
    }
    return socketTimeout;
  }

  public void init() {
    if (this.config.getSkipInit()) {
      logger.info("Skipping API initialization processes");
      return;
    }
    this.ctx.getRestfulClientFactory().setSocketTimeout(getSocketTimout());
    this.loadMeasureDefinitions();
    this.loadSearchParameters();
  }

}
