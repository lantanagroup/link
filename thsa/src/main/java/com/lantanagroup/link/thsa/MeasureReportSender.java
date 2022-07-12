package com.lantanagroup.link.thsa;

import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.IReportSender;
import org.hl7.fhir.r4.model.MeasureReport;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

public class MeasureReportSender implements IReportSender {
  @Override
  public void send(MeasureReport masterMeasureReport, HttpServletRequest request, Authentication auth, FhirDataProvider fhirDataProvider, Boolean sendWholeBundle, boolean removeGeneratedObservations) throws Exception {
    // TODO: Construct new FhirDataProvider for the consumer (take into account authentication)

    // TODO: updateResource(masterMeasureReport) on FhirDataProvider for consumer
  }
}
