package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.GenericSender;
import com.lantanagroup.link.IReportSender;
import com.lantanagroup.link.auth.LinkCredentials;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;


@Component
public class FHIRSender extends GenericSender implements IReportSender {
  protected static final Logger logger = LoggerFactory.getLogger(FHIRSender.class);

  @Override
  public void send(MeasureReport masterMeasureReport, HttpServletRequest request, Authentication auth, FhirDataProvider fhirDataProvider, Boolean sendWholeBundle, boolean removeGeneratedObservations) throws Exception {

    sendContent(masterMeasureReport, fhirDataProvider, sendWholeBundle, removeGeneratedObservations);

    FhirHelper.recordAuditEvent(request, fhirDataProvider, ((LinkCredentials) auth.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.Send, "Successfully sent report");
  }

  public String bundle(Bundle bundle, FhirDataProvider fhirDataProvider, String type) {
    if(type.equals("json")) {
      return fhirDataProvider.bundleToJson(bundle);
    }

    if(type.equals("xml")) {
      return fhirDataProvider.bundleToXml(bundle);
    }

    if(type.equals("csv")) {
      // TODO: Use Keith's CSV conversion code to convert Bundle to CSV
      String csv = "";
      return csv;
    }

    return "";
  }
}
