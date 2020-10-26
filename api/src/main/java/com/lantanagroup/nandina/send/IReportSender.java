package com.lantanagroup.nandina.send;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.JsonProperties;
import com.lantanagroup.nandina.QueryReport;

public interface IReportSender {
  void send(QueryReport report, JsonProperties config, FhirContext ctx) throws Exception;
}
