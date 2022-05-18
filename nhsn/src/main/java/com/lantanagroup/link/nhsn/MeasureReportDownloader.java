package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.FhirBundler;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.IReportDownloader;
import com.lantanagroup.link.config.api.ApiConfig;
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

public class MeasureReportDownloader implements IReportDownloader {
  protected static final Logger logger = LoggerFactory.getLogger(MeasureReportDownloader.class);

  @Override
  public void download(String reportId, FhirDataProvider fhirDataProvider, HttpServletResponse response, FhirContext ctx, ApiConfig config) throws IOException, TransformerException {

    DocumentReference docRefBundle = fhirDataProvider.findDocRefForReport(reportId);

    if (docRefBundle == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The report does not exist.");
    }

    MeasureReport measureReport = fhirDataProvider.getMeasureReportById(reportId);
    // MeasureReport measureReport = fhirStoreClient.read().resource(MeasureReport.class).withId(reportId).execute();

    if (measureReport == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The report does not have a MeasureReport");
    }

    logger.info("Building Bundle for MeasureReport...");
    //Bundle bundle = FhirHelper.bundleMeasureReport(measureReport, fhirDataProvider, config.getSendWholeBundle() != null ? config.getSendWholeBundle() : true);

    FhirBundler bundler = new FhirBundler(fhirDataProvider);
    Bundle bundle = bundler.generateBundle(config.getSendWholeBundle() != null ? config.getSendWholeBundle() : true, config.isRemoveGeneratedObservations(), measureReport);

    logger.info("Bundle created for MeasureReport including " + bundle.getEntry().size() + " entries");

    String responseBody = ctx.newXmlParser().encodeResourceToString(bundle);
    response.setContentType("application/xml");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + reportId + ".xml\"");

    InputStream is = new ByteArrayInputStream(responseBody.getBytes());
    IOUtils.copy(is, response.getOutputStream());
    response.flushBuffer();
  }
}
