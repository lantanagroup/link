package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.*;
import com.lantanagroup.link.config.bundler.BundlerConfig;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MeasureReportDownloader implements IReportDownloader {
  protected static final Logger logger = LoggerFactory.getLogger(MeasureReportDownloader.class);

  @Override
  public void download(String reportId, String downloadType, FhirDataProvider fhirDataProvider, HttpServletResponse response, FhirContext ctx, BundlerConfig config, EventService eventService) throws IOException, TransformerException {

    DocumentReference docRefBundle = fhirDataProvider.findDocRefForReport(reportId.contains("-") ? reportId.substring(0, reportId.indexOf('-')) : reportId);

    if (docRefBundle == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The report does not exist.");
    }

    MeasureReport measureReport = fhirDataProvider.getMeasureReportById(reportId);
    // MeasureReport measureReport = fhirStoreClient.read().resource(MeasureReport.class).withId(reportId).execute();

    if (measureReport == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The report does not have a MeasureReport");
    }

    logger.info("Building Bundle for MeasureReport...");
    FhirBundler bundler = new FhirBundler(config, fhirDataProvider, eventService);
    Bundle bundle = bundler.generateBundle(List.of(measureReport), docRefBundle);

    logger.info("Bundle created for MeasureReport including " + bundle.getEntry().size() + " entries");
    String responseBody = "";
    if (downloadType.equals("XML")) {
      responseBody = ctx.newXmlParser().encodeResourceToString(bundle);
      response.setContentType("application/xml");
    } else if (downloadType.equals("JSON")) {
      responseBody = ctx.newJsonParser().encodeResourceToString(bundle);
      response.setContentType("application/json");
    }

    if (Helper.validateHeaderValue(reportId)) {
      response.setHeader("Content-Disposition", "attachment; filename=\"" + reportId + ".xml\"");
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report Id");
    }

    InputStream is = new ByteArrayInputStream(responseBody.getBytes());
    IOUtils.copy(is, response.getOutputStream());
    response.flushBuffer();
  }
}
