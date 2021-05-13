package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.QueryReport;
import com.lantanagroup.link.config.api.ApiConfig;

public interface IReportSender {
  void send(QueryReport report, ApiConfig config, FhirContext ctx) throws Exception;
}
