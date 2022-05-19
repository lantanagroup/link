package com.lantanagroup.link;

import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.sender.FHIRSenderConfig;
import com.lantanagroup.link.config.sender.FhirSenderUrlOAuthConfig;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

public abstract class GenericSender {
  protected static final Logger logger = LoggerFactory.getLogger(GenericSender.class);

  @Autowired
  @Setter
  private FHIRSenderConfig config;

  public Bundle generateBundle(DocumentReference documentReference, MeasureReport masterMeasureReport, FhirDataProvider fhirProvider, boolean sendWholeBundle, boolean removeGeneratedObservations, String location) {
    logger.info("Building Bundle for MeasureReport to send...");

    FhirBundler bundler = new FhirBundler(fhirProvider);

    Bundle bundle = bundler.generateBundle(sendWholeBundle, removeGeneratedObservations, masterMeasureReport, documentReference);

    //String existingLocation = getDocumentLocation(masterMeasureReport, fhirProvider);

    if (!location.equals("")) {
      bundle.setId(location);
    }
    return bundle;
  }

  public HttpClient getHttpClient() {
    return HttpClientBuilder.create().build();
  }


  public abstract String bundle(Bundle bundle, FhirDataProvider fhirProvider);

  public String sendContent(MeasureReport masterMeasureReport, FhirDataProvider fhirProvider, String mimeType,
                            boolean sendWholeBundle, boolean removeGeneratedObservations) throws Exception {

    String location = "";

    if (this.config.getSendUrls() == null || this.config.getSendUrls().isEmpty()) {
      throw new Exception("Not configured with any locations to send");
    }

    logger.trace(String.format("Configured to send to %s locations", this.config.getSendUrls().size()));

    for (FhirSenderUrlOAuthConfig authConfig : this.config.getSendUrls()) {
      logger.info("Sending MeasureReport bundle to URL " + authConfig.getUrl());
      DocumentReference documentReference = fhirProvider.findDocRefForReport(masterMeasureReport.getIdElement().getIdPart());
      String existingLocation = FhirHelper.getDocumentReferenceLocationByUrl(documentReference, authConfig.getUrl());

      Bundle bundle = generateBundle(documentReference, masterMeasureReport, fhirProvider, sendWholeBundle, removeGeneratedObservations, existingLocation);

      String content = bundle(bundle, fhirProvider);

      String token = OAuth2Helper.getToken(authConfig.getAuthConfig(), getHttpClient());

      // decide to do a POST or a PUT
      HttpEntityEnclosingRequestBase sendRequest = null;
      if (existingLocation.equals("")) {
        sendRequest = new HttpPost(authConfig.getUrl());
      } else {
        sendRequest = new HttpPut(existingLocation);
      }
      sendRequest.addHeader("Content-Type", mimeType);

      // set request entity with optional compression
      HttpEntity entity = new StringEntity(content);
      if (authConfig.isCompress()) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
          try (GZIPOutputStream compressedStream = new GZIPOutputStream(stream)) {
            entity.writeTo(compressedStream);
          }
          HttpEntity compressedEntity = new ByteArrayEntity(stream.toByteArray());
          sendRequest.setEntity(compressedEntity);
          sendRequest.addHeader("Content-Encoding", "gzip");
        }
      } else {
        sendRequest.setEntity(entity);
      }

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
          this.updateDocumentLocation(masterMeasureReport, fhirProvider, location);
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

  public void updateDocumentLocation(MeasureReport masterMeasureReport, FhirDataProvider fhirDataProvider, String
          location) {
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

}
