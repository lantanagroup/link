package com.lantanagroup.link;

import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.sender.FHIRSenderConfig;
import com.lantanagroup.link.config.sender.FhirSenderUrlOAuthConfig;
import lombok.Setter;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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


  public abstract String bundle(Bundle bundle, FhirDataProvider fhirProvider, String type);

  public String sendContent(MeasureReport masterMeasureReport, FhirDataProvider fhirProvider,
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
      String token = OAuth2Helper.getToken(authConfig.getAuthConfig(), getHttpClient());
      Bundle bundle = generateBundle(documentReference, masterMeasureReport, fhirProvider, sendWholeBundle, removeGeneratedObservations, existingLocation);

      boolean hasBeenSent;
      if(existingLocation.equals("")) {
        hasBeenSent = false;
        location = authConfig.getUrl();
      }
      else {
        hasBeenSent = true;
        location = existingLocation;
      }
      fhirProvider.submitToServer(locationCleaner(location, hasBeenSent), token, bundle);
    }
    return location;
  }

  private String locationCleaner(String location, boolean hasBeenSent) {
    String cleanLocation = location.contains("/history")?location.substring(0, location.indexOf("/history")):location;
    String[] locationParts = cleanLocation.split("/");
    int idTrim = hasBeenSent?2:1;
    return locationParts.length > idTrim?location.substring(0, location.indexOf(locationParts[locationParts.length - idTrim])):location;
  }

  public void updateDocumentLocation(MeasureReport masterMeasureReport, FhirDataProvider fhirDataProvider, String location) {
    String reportID = masterMeasureReport.getIdElement().getIdPart();
    DocumentReference documentReference = fhirDataProvider.findDocRefForReport(reportID);
    if (documentReference != null) {
      String previousLocation = FhirHelper.getFirstDocumentReferenceLocation(documentReference);
      if(previousLocation != null && !previousLocation.equals("")) {
        for (int index = documentReference.getContent().size() - 1; index > -1; index--) {
          if (documentReference.getContent().get(index).hasAttachment() && documentReference.getContent().get(index).getAttachment().hasUrl()) {
            documentReference.getContent().remove(index);
          }
        }
      }
      documentReference.getContent().add(new DocumentReference.DocumentReferenceContentComponent());
      Attachment attachment = new Attachment();
      attachment.setUrl(location);
      documentReference.getContent().get(documentReference.getContent().size() - 1).setAttachment(attachment);
      fhirDataProvider.updateResource(documentReference);
    }
  }

}
