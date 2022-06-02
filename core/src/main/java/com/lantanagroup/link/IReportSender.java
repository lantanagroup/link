package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.config.api.ApiConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

public interface IReportSender {
  void send(MeasureReport masterMeasureReport, HttpServletRequest request, Authentication auth, FhirDataProvider fhirDataProvider, Boolean sendWholeBundle, boolean removeGeneratedObservations) throws Exception;

  /**
   * Looks up submission location from document reference to send a request for the previously submitted bundle which is returns
   *
   * @param config used to authenticate based on configuration and attach authentication token to submission request
   * @param fhirContext used to parse the content body of the response from the submission request into a bundle to return
   * @param existingDocumentReference used to look up the address of where the previous submission was sent
   * @return returns the submission bundle that was previously sent
   */
  Bundle retrieve(ApiConfig config, FhirContext fhirContext, DocumentReference existingDocumentReference);
}
