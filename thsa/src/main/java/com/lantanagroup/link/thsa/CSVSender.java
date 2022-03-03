package com.lantanagroup.link.thsa;

import com.ainq.saner.converters.SanerCSVConverter;
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

import javax.servlet.http.HttpServletRequest;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;

public class CSVSender extends GenericSender implements IReportSender {
  protected static final Logger logger = LoggerFactory.getLogger(CSVSender.class);


  // Map containing mappings population codes in the MeasureReport and headings in the CSV (if not using the codes)
  protected Map<String, String> orderedHeaderMap = new TreeMap<>();

  public Map<String, String> getOrderedHeaderMap() {
    return orderedHeaderMap;
  }

  public void setOrderedHeaderMap(Map<String, String> orderedHeaderMap) {
    this.orderedHeaderMap = orderedHeaderMap;
  }

  @Override
  public void send(MeasureReport masterMeasureReport, HttpServletRequest request, Authentication auth, FhirDataProvider fhirDataProvider, Boolean sendWholeBundle) throws Exception {
    Bundle bundle = this.generateBundle(masterMeasureReport, fhirDataProvider, sendWholeBundle);
    logger.info("Bundle created for MeasureReport including " + bundle.getEntry().size() + " entries");
    StringWriter csvWriter = new StringWriter();
    SanerCSVConverter.convertMeasureReportToCSV(masterMeasureReport,this.orderedHeaderMap, new PrintWriter(csvWriter),true);
    String csv = csvWriter.toString();
    String location = this.sendContent(csv, "text/csv");
    if(!"".equals(location)) {
      updateDocumentLocation(masterMeasureReport, fhirDataProvider, location);
    }

    FhirHelper.recordAuditEvent(request, fhirDataProvider, ((LinkCredentials) auth.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.Send, "Successfully sent report");
  }
}
