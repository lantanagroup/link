package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.bundler.BundlerConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IReportSender {
  void send(List<MeasureReport> masterMeasureReports, DocumentReference documentReference, HttpServletRequest request, Authentication auth, FhirDataProvider fhirDataProvider, BundlerConfig bundlerConfig) throws Exception;
}
