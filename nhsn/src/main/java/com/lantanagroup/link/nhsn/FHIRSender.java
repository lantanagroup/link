package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.GenericSender;
import com.lantanagroup.link.IReportSender;
import com.lantanagroup.link.auth.LinkCredentials;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;


@Component
public class FHIRSender extends GenericSender implements IReportSender {
  protected static final Logger logger = LoggerFactory.getLogger(FHIRSender.class);

  @Override
  public void send(MeasureReport masterMeasureReport, DocumentReference documentReference, HttpServletRequest request, Authentication auth, FhirDataProvider fhirDataProvider, Boolean sendWholeBundle, boolean removeGeneratedObservations) throws Exception {
    Bundle bundle = this.generateBundle(documentReference, masterMeasureReport, fhirDataProvider, sendWholeBundle, removeGeneratedObservations);
    String location = this.sendContent(bundle, documentReference, fhirDataProvider);

    if (StringUtils.isNotEmpty(location)) {
      FhirHelper.setSubmissionLocation(documentReference, location);
    }

    FhirHelper.recordAuditEvent(request, fhirDataProvider, ((LinkCredentials) auth.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.Send, "Successfully sent report");
  }

  public String bundle(Bundle bundle, FhirDataProvider fhirDataProvider, String type) {
    if(type.equals("json")) {
      return fhirDataProvider.bundleToJson(bundle);
    }
    else if(type.equals("xml")) {
      return fhirDataProvider.bundleToXml(bundle);
    }
    else if(type.equals("csv")) {
      // TODO: Use Keith's CSV conversion code to convert Bundle to CSV
      return "";
    }
    else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing type, needs to be json, xml, or csv.");
    }
  }
}
