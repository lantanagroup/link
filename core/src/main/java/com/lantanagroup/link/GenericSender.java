package com.lantanagroup.link;

import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.sender.FHIRSenderConfig;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GenericSender {
  protected static final Logger logger = LoggerFactory.getLogger(GenericSender.class);

  @Autowired
  @Setter
  private FHIRSenderConfig config;

  public Bundle generateBundle(MeasureReport masterMeasureReport, FhirDataProvider fhirProvider, boolean sendWholeBundle) {
    logger.info("Building Bundle for MeasureReport to send...");

    FhirBundler bundler = new FhirBundler(fhirProvider);
    return bundler.generateBundle(sendWholeBundle, masterMeasureReport);
  }

  public HttpClient getHttpClient() {
    return HttpClientBuilder.create().build();
  }

  private String getToken() throws Exception {
    String token = "";

    if (this.config.getOAuthConfig() != null && this.config.getOAuthConfig().hasCredentialProperties()) {
      logger.info("Configured to authentication when submitting. Requesting a token from configured token URL");

      switch (this.config.getOAuthConfig().getCredentialMode()) {
        case Client:
          token = OAuth2Helper.getClientCredentialsToken(
                  this.getHttpClient(),
                  this.config.getOAuthConfig().getTokenUrl(),
                  this.config.getOAuthConfig().getUsername(),
                  this.config.getOAuthConfig().getPassword(),
                  this.config.getOAuthConfig().getScope());
          break;
        case Password:
          token = OAuth2Helper.getPasswordCredentialsToken(
                  this.getHttpClient(),
                  this.config.getOAuthConfig().getTokenUrl(),
                  this.config.getOAuthConfig().getUsername(),
                  this.config.getOAuthConfig().getPassword(),
                  this.config.getOAuthConfig().getClientId(),
                  this.config.getOAuthConfig().getScope());
      }
    } else {
      throw new Exception("Authentication is required to submit");
    }

    return token;
  }

  public String sendContent(String content, String mimeType) throws Exception {

    String location = "";

    if (this.config.getSendUrls() == null || this.config.getSendUrls().isEmpty()) {
      throw new Exception("Not configured with any locations to send");
    }

    logger.trace(String.format("Configured to send to %s locations", this.config.getSendUrls().size()));

    String token = this.getToken();

    for (String sendUrl : this.config.getSendUrls()) {
      logger.info("Sending MeasureReport bundle to URL " + sendUrl);

      HttpPost sendRequest = new HttpPost(sendUrl);
      sendRequest.addHeader("Content-Type", mimeType);

      if (Strings.isNotEmpty(token)) {
        logger.debug("Adding auth token to submit request");
        sendRequest.addHeader("Authorization", "Bearer " + token);
      }

      sendRequest.setEntity(new StringEntity(content));

      try {
        HttpClient httpClient = this.getHttpClient();

        HttpResponse response = httpClient.execute(sendRequest);

        if (response.getStatusLine().getStatusCode() >= 300) {
          String responseContent = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
          logger.error(String.format("Error (%s) submitting report to %s: %s", response.getStatusLine().getStatusCode(), sendUrl, responseContent));
          throw new HttpResponseException(500, "Internal Server Error");
        }

        if (response.getHeaders("Location") != null) {
          location = response.getHeaders("Location")[0].getElements()[0].getName();
          if (location.indexOf("/_history/") > 0) {
            location = location.substring(0, location.indexOf("/_history/"));
          }
          logger.debug("Response location is " + location);

        } else {
          logger.error("No Location header provided in response to submission");
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
      documentReference.getContent().get(0).getAttachment().setUrl(location);
      fhirDataProvider.updateResource(documentReference);
    }
  }
}
