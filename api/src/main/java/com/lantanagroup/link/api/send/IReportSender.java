package com.lantanagroup.link.api.send;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.QueryReport;
import com.lantanagroup.link.api.config.ApiConfig;

public interface IReportSender {
  void send(QueryReport report, ApiConfig config, FhirContext ctx) throws Exception;
}
