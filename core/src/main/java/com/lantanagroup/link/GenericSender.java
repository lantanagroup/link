package com.lantanagroup.link;

import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.sender.FHIRSenderConfig;
import com.lantanagroup.link.config.sender.FHIRSenderOAuthConfig;
import com.lantanagroup.link.config.sender.FhirSenderUrlOAuthConfig;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public abstract class GenericSender {
  protected static final Logger logger = LoggerFactory.getLogger(GenericSender.class);

  @Autowired
  @Setter
  private FHIRSenderConfig config;

  public Bundle generateBundle(MeasureReport masterMeasureReport, FhirDataProvider fhirProvider, boolean sendWholeBundle, String location) {
    logger.info("Building Bundle for MeasureReport to send...");

    FhirBundler bundler = new FhirBundler(fhirProvider);

    Bundle bundle = bundler.generateBundle(sendWholeBundle, masterMeasureReport);

    //String existingLocation = getDocumentLocation(masterMeasureReport, fhirProvider);

    if (!location.equals("")) {
      bundle.setId(location);
    }
    return bundle;
  }

  public HttpClient getHttpClient() {
    return HttpClientBuilder.create().build();
  }

  private String getToken(FHIRSenderOAuthConfig authConfig) throws Exception {
    String token = "";

    if (authConfig != null && authConfig.hasCredentialProperties()) {
      logger.info("Configured to authentication when submitting. Requesting a token from configured token URL");

      switch (authConfig.getCredentialMode()) {
        case Client:
          token = OAuth2Helper.getClientCredentialsToken(
                  this.getHttpClient(),
                  authConfig.getTokenUrl(),
                  authConfig.getUsername(),
                  authConfig.getPassword(),
                  authConfig.getScope());
          break;
        case Password:
          token = OAuth2Helper.getPasswordCredentialsToken(
                  this.getHttpClient(),
                  authConfig.getTokenUrl(),
                  authConfig.getUsername(),
                  authConfig.getPassword(),
                  authConfig.getClientId(),
                  authConfig.getScope());
      }
    } else {
      throw new Exception("Authentication is required to submit");
    }

    return token;
  }


  public abstract String bundle(Bundle bundle, FhirDataProvider fhirProvider);


  public String sendContent(MeasureReport masterMeasureReport, FhirDataProvider fhirProvider, String mimeType, boolean sendWholeBundle) throws Exception {

    String location = "";

    if (this.config.getSendUrls() == null || this.config.getSendUrls().isEmpty()) {
      throw new Exception("Not configured with any locations to send");
    }

    logger.trace(String.format("Configured to send to %s locations", this.config.getSendUrls().size()));


    for (FhirSenderUrlOAuthConfig authConfig : this.config.getSendUrls()) {
      logger.info("Sending MeasureReport bundle to URL " + authConfig.getUrl());

      String existingLocation = getDocumentLocation(masterMeasureReport, fhirProvider, authConfig.getUrl());

      Bundle bundle = generateBundle(masterMeasureReport, fhirProvider, sendWholeBundle, existingLocation);

      String content = bundle(bundle, fhirProvider);

      String token = this.getToken(authConfig.getAuthConfig());

      // decide to do a POST or a PUT
      HttpRequestBase sendRequest = null;
      if (existingLocation.equals("")) {
        sendRequest = new HttpPost(authConfig.getUrl());
        ((HttpPost) sendRequest).setEntity(new StringEntity(content));
      } else {
        sendRequest = new HttpPut(existingLocation);
        ((HttpPut) sendRequest).setEntity(new StringEntity(content));
      }
      sendRequest.addHeader("Content-Type", mimeType);

      if (Strings.isNotEmpty(token)) {
        logger.debug("Adding auth token to submit request");
        sendRequest.addHeader("Authorization", "Bearer " + token);
      }

      try {
        HttpClient httpClient = this.getHttpClient();

        HttpResponse response = httpClient.execute(sendRequest);

        if (response.getStatusLine().getStatusCode() >= 300) {
          String responseContent = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
          logger.error(String.format("Error (%s) submitting report to %s: %s", response.getStatusLine().getStatusCode(), authConfig.getUrl(), responseContent));
          throw new HttpResponseException(500, "Internal Server Error");
        }

        if (response.getHeaders("Location") != null && response.getHeaders("Location").length > 0) {
          location = response.getHeaders("Location")[0].getElements()[0].getName();
          if (location.indexOf("/_history/") > 0) {
            location = location.substring(0, location.indexOf("/_history/"));
          }
          logger.debug("Response location is " + location);
          // update the location on the DocumentReference
          updateDocumentLocation(masterMeasureReport, fhirProvider, location);
        }

      } catch (IOException ex) {
        if (ex.getMessage().contains("403")) {
          logger.error("Error authorizing send: " + ex.getMessage());
        } else {
          logger.error("Error while sending MeasureReport bundle to URL", ex);
        }
        throw ex;
      }
    }
    return location;
  }

  public void updateDocumentLocation(MeasureReport masterMeasureReport, FhirDataProvider fhirDataProvider, String location) {
    String reportID = masterMeasureReport.getIdElement().getIdPart();
    DocumentReference documentReference = fhirDataProvider.findDocRefForReport(reportID);
    if (documentReference != null) {
      documentReference.getContent().add(new DocumentReference.DocumentReferenceContentComponent());
      Attachment attachment = new Attachment();
      attachment.setUrl(location);
      documentReference.getContent().get(documentReference.getContent().size() - 1).setAttachment(attachment);
      fhirDataProvider.updateResource(documentReference);
    }
  }

  public String getDocumentLocation(MeasureReport masterMeasureReport, FhirDataProvider fhirDataProvider, String sendUrl) {
    String reportID = masterMeasureReport.getIdElement().getIdPart();
    String location = "";
    DocumentReference documentReference = fhirDataProvider.findDocRefForReport(reportID);
    if (documentReference != null) {
      Optional<DocumentReference.DocumentReferenceContentComponent> loc = documentReference.getContent().stream().filter(content -> content.getAttachment().getUrl().contains(sendUrl)).findFirst();
      if (loc.isPresent()) {
        location = loc.get().getAttachment().getUrl() != null ? loc.get().getAttachment().getUrl() : "";
      }
    }
    return location;
  }
}
