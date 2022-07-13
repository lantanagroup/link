package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.config.api.ApiConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

public interface IReportSender {
  void send(MeasureReport masterMeasureReport, DocumentReference documentReference, HttpServletRequest request, Authentication auth, FhirDataProvider fhirDataProvider, Boolean sendWholeBundle, boolean removeGeneratedObservations) throws Exception;
}
