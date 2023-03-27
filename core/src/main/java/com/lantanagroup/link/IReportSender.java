package com.lantanagroup.link;

import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.bundler.BundlerConfig;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IReportSender {
  void send(List<MeasureReport> masterMeasureReports, DocumentReference documentReference, HttpServletRequest request, LinkCredentials user, FhirDataProvider fhirDataProvider, BundlerConfig bundlerConfig) throws Exception;
}
