package com.lantanagroup.nandina.api.send;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.QueryReport;
import com.lantanagroup.nandina.api.config.ApiConfig;

public interface IReportSender {
  void send(QueryReport report, ApiConfig config, FhirContext ctx) throws Exception;
}
