package com.lantanagroup.link.thsa;

import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.GenericSender;
import com.lantanagroup.link.IReportSender;
import com.lantanagroup.link.auth.LinkCredentials;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

public class CSVSender extends GenericSender implements IReportSender {
  protected static final Logger logger = LoggerFactory.getLogger(CSVSender.class);

  @Override
  public void send(MeasureReport masterMeasureReport, HttpServletRequest request, Authentication auth, FhirDataProvider fhirDataProvider, Boolean sendWholeBundle) throws Exception {
    Bundle bundle = this.generateBundle(masterMeasureReport, fhirDataProvider, sendWholeBundle);

    logger.info("Bundle created for MeasureReport including " + bundle.getEntry().size() + " entries");

    // TODO: Use Keith's CSV conversion code to convert Bundle to CSV

    String csv = "";

    String location = this.sendContent(csv, "text/csv");

    if(!"".equals(location)) {
      updateDocumentLocation(masterMeasureReport, fhirDataProvider, location);
    }

    FhirHelper.recordAuditEvent(request, fhirDataProvider, ((LinkCredentials) auth.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.Send, "Successfully sent report");
  }
}
