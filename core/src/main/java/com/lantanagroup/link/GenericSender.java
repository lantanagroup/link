package com.lantanagroup.link;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.apache.GZipContentInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.bundler.BundlerConfig;
import com.lantanagroup.link.config.sender.FHIRSenderConfig;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class GenericSender {
  protected static final Logger logger = LoggerFactory.getLogger(GenericSender.class);

  // TODO: This should be re-factored out of this GenericSender class
  @Autowired
  @Setter
  private FHIRSenderConfig fhirSenderConfig;

  @Autowired
  private EventService eventService;

  public Bundle generateBundle(DocumentReference documentReference, List<MeasureReport> masterMeasureReports, FhirDataProvider fhirProvider, BundlerConfig bundlerConfig) {
    logger.info("Building Bundle for MeasureReport to send...");
    FhirBundler bundler = new FhirBundler(bundlerConfig, fhirProvider, eventService);
    Bundle bundle = bundler.generateBundle(masterMeasureReports, documentReference);
    logger.info(String.format("Done building Bundle for MeasureReport with %s entries", bundle.getEntry().size()));
    return bundle;
  }

  public CloseableHttpClient getHttpClient() {
    return HttpClientBuilder.create().build();
  }

  public abstract String bundle(Bundle bundle, FhirDataProvider fhirProvider, String type);

  public String sendContent(Resource resourceToSend, DocumentReference documentReference, FhirDataProvider fhirStoreProvider) throws Exception {

    if (StringUtils.isEmpty(this.fhirSenderConfig.getUrl())) {
      throw new IllegalStateException("Not configured with any locations to send");
    }

    Resource copy = resourceToSend.copy();
    IGenericClient client = fhirStoreProvider.ctx.newRestfulGenericClient(this.fhirSenderConfig.getUrl());
    client.registerInterceptor(new GZipContentInterceptor());

    String token = OAuth2Helper.getToken(this.fhirSenderConfig.getAuthConfig(), getHttpClient());

    if (StringUtils.isNotEmpty(token)) {
      BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(token);
      client.registerInterceptor(authInterceptor);
    }

    FhirDataProvider consumerFhirProvider = new FhirDataProvider(client);

    Optional<DocumentReference.DocumentReferenceContentComponent> content =
            documentReference.getContent().stream().filter(c -> c.hasAttachment() && c.getAttachment().hasUrl()).collect(Collectors.toList()).stream().findFirst();

    // If we've already submitted to the server and the base url of where it was submitted hasn't changed
    if (content.isPresent() && content.get().getAttachment().getUrl().toLowerCase(Locale.ENGLISH).startsWith(this.fhirSenderConfig.getUrl().toLowerCase(Locale.ENGLISH))) {
      String location = locationCleaner(content.get().getAttachment().getUrl());
      copy.setId(location);
    }

    logger.info("Sending MeasureReport bundle to URL " + this.fhirSenderConfig.getUrl());

    MethodOutcome outcome;

    if (copy.hasId()) {
      outcome = consumerFhirProvider.updateResource(copy);
    } else {
      outcome = consumerFhirProvider.createOutcome(copy);
    }

    List<String> locations = outcome.getResponseHeaders().get("content-location");

    if (locations != null && locations.size() == 1) {
      return locations.get(0);
    }

    return null;
  }

  private String locationCleaner(String location) {
    String cleanLocation = location.contains("/history")?location.substring(0, location.indexOf("/history")):location;
    return cleanLocation;
  }

  public void updateDocumentLocation(MeasureReport masterMeasureReport, FhirDataProvider fhirStoreProvider, String location) {
    String reportID = masterMeasureReport.getIdElement().getIdPart();
    DocumentReference documentReference = fhirStoreProvider.findDocRefForReport(reportID);
    if (documentReference != null) {
      String previousLocation = FhirHelper.getSubmittedLocation(documentReference);
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
      fhirStoreProvider.updateResource(documentReference);
    }
  }

}
