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
public class JSONSender extends GenericSender implements IReportSender {
  protected static final Logger logger = LoggerFactory.getLogger(JSONSender.class);

  @Override
  public void send(MeasureReport masterMeasureReport, HttpServletRequest request, Authentication auth, FhirDataProvider fhirDataProvider, Boolean sendWholeBundle) throws Exception {
    Bundle bundle = this.generateBundle(masterMeasureReport, fhirDataProvider, sendWholeBundle);

    logger.info("Bundle created for MeasureReport including " + bundle.getEntry().size() + " entries");

    String json = fhirDataProvider.bundleToJson(bundle);

    String location = this.sendContent(json, "application/json");

    if(!"".equals(location)) {
      updateDocumentLocation(masterMeasureReport, fhirDataProvider, location);
    }

    FhirHelper.recordAuditEvent(request, fhirDataProvider, ((LinkCredentials) auth.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.Send, "Successfully sent report");
  }
}
