package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.GenericSender;
import com.lantanagroup.link.IReportSender;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.auth.LinkOAuthConfig;
import com.lantanagroup.link.config.sender.FhirSenderUrlOAuthConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;


@Component
public class FHIRSender extends GenericSender implements IReportSender {
  protected static final Logger logger = LoggerFactory.getLogger(FHIRSender.class);

  @Override
  public void send(MeasureReport masterMeasureReport, HttpServletRequest request, Authentication auth, FhirDataProvider fhirDataProvider, Boolean sendWholeBundle, boolean removeGeneratedObservations) throws Exception {

    sendContent(masterMeasureReport, fhirDataProvider, "application/xml", sendWholeBundle, removeGeneratedObservations);

    FhirHelper.recordAuditEvent(request, fhirDataProvider, ((LinkCredentials) auth.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.Send, "Successfully sent report");
  }

  @Override
  public Bundle retrieve(ApiConfig apiConfig, FhirContext fhirContext, DocumentReference existingDocumentReference) {
    String submittedUrl = existingDocumentReference.getContent().get(0).getAttachment().getUrl();
    if(submittedUrl != null) {
      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
              .uri(URI.create(submittedUrl));
      //.setHeader("if-modified-since", lastUpdateDate);

      LinkOAuthConfig authConfig = apiConfig.getReportDefs().getAuth();
      if (authConfig != null) {
        try {
          String token = OAuth2Helper.getToken(authConfig);
          requestBuilder.setHeader("Authorization", "Bearer " + token);
        } catch (Exception ex) {
          logger.error(String.format("Error generating authorization token: %s", ex.getMessage()));
          return null;
        }
      }

      HttpRequest submissionReq = requestBuilder.build();
      // TODO: Authenticate based on configuration
      // TODO: Attach authentication token to submissionReq
      //HttpResponse submissionRes = submissionReq.getResponse();
      // TODO: Read response
    /*FHIRReceiver receiver = new FHIRReceiver();
    String bundleLocation = FhirHelper.getFirstDocumentReferenceLocation(existingDocumentReference);
    String content = null;
    try {
      content = receiver.retrieveContent(bundleLocation);
    } catch (Exception e) {
      e.printStackTrace();
    }*/
    }

    //return (Bundle) fhirContext.newJsonParser().parseResource(content);
    return new Bundle();
  }

  public String bundle(Bundle bundle, FhirDataProvider fhirDataProvider) {
    return fhirDataProvider.bundleToXml(bundle);
  }
}
