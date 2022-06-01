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
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    HttpClient client = HttpClient.newHttpClient();
    String bundleLocation = FhirHelper.getFirstDocumentReferenceLocation(existingDocumentReference);
    if(bundleLocation != null && !bundleLocation.equals("")) {
      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
              .uri(URI.create(bundleLocation));

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
      requestBuilder.GET();
      HttpRequest submissionReq = requestBuilder.build();

      HttpResponse<String> response = null;
      try {
        response = client
                .send(submissionReq, HttpResponse.BodyHandlers.ofString());
      } catch (IOException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      if(response != null) {
        return (Bundle) fhirContext.newJsonParser().parseResource(response.body());
      }
    }
    return null;
  }

  public String bundle(Bundle bundle, FhirDataProvider fhirDataProvider) {
    return fhirDataProvider.bundleToXml(bundle);
  }
}
