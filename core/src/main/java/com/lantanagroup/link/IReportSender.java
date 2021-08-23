package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.config.api.ApiConfig;
import org.hl7.fhir.r4.model.MeasureReport;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

public interface IReportSender {
  void send (MeasureReport report, FhirContext ctx, HttpServletRequest request, Authentication auth, IGenericClient fhirStoreClient) throws Exception;
}
