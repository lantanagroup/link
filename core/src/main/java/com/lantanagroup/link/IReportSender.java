package com.lantanagroup.link;

import org.hl7.fhir.r4.model.MeasureReport;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

public interface IReportSender {
  void send(MeasureReport masterMeasureReport, HttpServletRequest request, Authentication auth, FhirDataProvider fhirDataProvider, Boolean sendWholeBundle) throws Exception;
}
