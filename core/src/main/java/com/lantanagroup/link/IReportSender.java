package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.config.api.ApiConfig;
import org.hl7.fhir.r4.model.MeasureReport;

public interface IReportSender {
  void send (MeasureReport report, ApiConfig config, FhirContext ctx) throws Exception;
}
